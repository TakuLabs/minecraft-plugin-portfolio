package de.otaku.empirecrates.util;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModelEngineBridge {
    private final Plugin plugin;
    private boolean warned;
    private boolean animationDiagnosticsLogged;

    public ModelEngineBridge(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().isPluginEnabled("ModelEngine");
    }

    public ModelHandle spawnModel(Entity baseEntity, String modelId) {
        if (modelId == null || modelId.isBlank() || !isAvailable()) {
            return null;
        }
        try {
            Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Object modeledEntity = api.getMethod("createModeledEntity", Entity.class).invoke(null, baseEntity);
            Object activeModel = api.getMethod("createActiveModel", String.class).invoke(null, modelId);
            if (modeledEntity == null || activeModel == null) {
                warn("ModelEngine returned no model for '" + modelId + "'. Check the exact model ID and whether ModelEngine finished loading.");
                return null;
            }
            invokeAnyNoArgs(modeledEntity, "clearModels", "removeAllModels");
            if (!invokeAddModel(modeledEntity, activeModel)) {
                warn("ModelEngine addModel method was not found.");
                return null;
            }
            invokeLifecycleSync(modeledEntity);
            invokeLifecycleSync(activeModel);
            return new ModelHandle(baseEntity.getUniqueId(), modelId, modeledEntity, activeModel);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warn("Could not spawn ModelEngine model '" + modelId + "': " + exception.getMessage());
            return null;
        }
    }

    public boolean applyModel(Entity baseEntity, String modelId) {
        return spawnModel(baseEntity, modelId) != null;
    }

    public List<String> availableModelIds() {
        Set<String> ids = new LinkedHashSet<>();
        if (isAvailable()) {
            collectModelIdsFromApi(ids);
        }
        collectModelIdsFromFiles(ids);
        return sorted(ids);
    }

    public List<String> animationIds(String modelId) {
        Set<String> ids = new LinkedHashSet<>();
        if (modelId != null && !modelId.isBlank() && isAvailable()) {
            collectAnimationIdsFromApi(modelId, ids);
        }
        collectAnimationIdsFromFiles(modelId, ids);
        ids.addAll(List.of("opening", "opening_rare", "idle", "idle_mouve", "closing", "none"));
        return sorted(ids);
    }

    public boolean playAnimation(ModelHandle handle, String animation, boolean loop) {
        if (handle == null || animation == null || animation.isBlank()) {
            return false;
        }
        Object animationHandler = animationHandler(handle.activeModel());
        if (animationHandler == null) {
            return false;
        }
        if (!usesPriorityStateMachine(animationHandler)) {
            stopAnimations(animationHandler);
        }
        if (invokePlay(animationHandler, animation, loop, loop ? 1 : 2)) {
            return true;
        }
        warn("Could not play ModelEngine animation '" + animation + "'.");
        return false;
    }

    public boolean playAnimation(ModelHandle handle, List<String> animations, boolean loop) {
        if (handle == null || animations == null || animations.isEmpty()) {
            return false;
        }
        Object animationHandler = animationHandler(handle.activeModel());
        if (animationHandler == null) {
            return false;
        }
        for (String animation : animations) {
            if (animation == null || animation.isBlank()) {
                continue;
            }
            if (!usesPriorityStateMachine(animationHandler)) {
                stopAnimations(animationHandler);
            }
            if (invokePlay(animationHandler, animation, loop, loop ? 1 : 2)) {
                return true;
            }
        }
        logAnimationDiagnostics(animationHandler, animations);
        return false;
    }

    public void destroyModel(ModelHandle handle) {
        if (handle == null) {
            return;
        }
        invokeAnyNoArgs(handle.activeModel(), "destroy", "remove", "despawn");
        invokeRemoveModel(handle);
        invokeAnyNoArgs(handle.modeledEntity(), "clearModels", "removeAllModels", "destroy", "remove", "despawn");
    }

    public void syncViewer(ModelHandle handle, Player player) {
        if (handle == null || player == null || !player.isOnline()) {
            return;
        }
        invokeViewerSync(handle.modeledEntity(), player);
        invokeViewerSync(handle.activeModel(), player);
    }

    public boolean isModelUsable(ModelHandle handle) {
        if (handle == null) {
            return false;
        }
        return objectLooksUsable(handle.modeledEntity()) && objectLooksUsable(handle.activeModel());
    }

    private boolean objectLooksUsable(Object target) {
        if (target == null) {
            return false;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != boolean.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            try {
                boolean value = (boolean) method.invoke(target);
                if ((name.equals("isremoved") || name.equals("isdestroyed") || name.equals("isdead") || name.equals("isdespawned")) && value) {
                    return false;
                }
                if ((name.equals("isspawned") || name.equals("isactive") || name.equals("isalive")) && !value) {
                    return false;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return true;
    }

    private void invokeLifecycleSync(Object target) {
        if (target == null) {
            return;
        }
        for (Method method : target.getClass().getMethods()) {
            if (method.getParameterCount() != 0) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (!name.equals("spawn") && !name.equals("forcespawn") && !name.equals("sync") && !name.equals("update")) {
                continue;
            }
            try {
                method.invoke(target);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private void invokeViewerSync(Object target, Player player) {
        if (target == null) {
            return;
        }
        for (Method method : target.getClass().getMethods()) {
            String name = method.getName().toLowerCase();
            if (name.contains("hide") || name.contains("remove") || name.contains("destroy")
                    || name.contains("despawn") || name.contains("clear")) {
                continue;
            }
            if (!name.contains("show") && !name.contains("spawn")
                    && !name.contains("syncviewer") && !name.contains("addviewer")
                    && !name.contains("starttracking") && !name.contains("forcevisible")) {
                continue;
            }
            Class<?>[] types = method.getParameterTypes();
            try {
                if (types.length == 1 && types[0].isAssignableFrom(Player.class)) {
                    method.invoke(target, player);
                    continue;
                }
                if (types.length == 2 && types[0].isAssignableFrom(Player.class) && isBooleanType(types[1])) {
                    method.invoke(target, player, true);
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private void invokeRemoveModel(ModelHandle handle) {
        Object modeledEntity = handle.modeledEntity();
        Object activeModel = handle.activeModel();
        for (Method method : modeledEntity.getClass().getMethods()) {
            if (!method.getName().equals("removeModel") || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            try {
                if (parameter == String.class) {
                    method.invoke(modeledEntity, handle.modelId());
                    return;
                }
                if (parameter.isAssignableFrom(activeModel.getClass())) {
                    method.invoke(modeledEntity, activeModel);
                    return;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private void invokeAnyNoArgs(Object target, String... methodNames) {
        if (target == null) {
            return;
        }
        for (String methodName : methodNames) {
            try {
                Method method = target.getClass().getMethod(methodName);
                method.invoke(target);
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private void stopAnimations(Object animationHandler) {
        for (String methodName : new String[]{"stopAllAnimations", "stopAnimations", "forceStopAllAnimations"}) {
            try {
                Method method = animationHandler.getClass().getMethod(methodName);
                method.invoke(animationHandler);
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        for (Method method : animationHandler.getClass().getMethods()) {
            if (!method.getName().equals("stopAnimation") || method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                continue;
            }
            try {
                method.invoke(animationHandler, "idle");
                method.invoke(animationHandler, "idle_mouve");
                method.invoke(animationHandler, "opening");
                method.invoke(animationHandler, "opening_rare");
                method.invoke(animationHandler, "closing");
                return;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private Object animationHandler(Object activeModel) {
        for (String methodName : new String[]{"getAnimationHandler", "getAnimation", "getAnimations"}) {
            try {
                Method method = activeModel.getClass().getMethod(methodName);
                return method.invoke(activeModel);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private boolean invokePlay(Object animationHandler, String animation, boolean loop, int priority) {
        for (Method method : animationHandler.getClass().getMethods()) {
            if (!method.getName().equals("playAnimation")) {
                continue;
            }
            try {
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 6
                        && isIntegerType(types[0])
                        && types[1] == String.class
                        && isNumberType(types[2])
                        && isNumberType(types[3])
                        && isNumberType(types[4])
                        && isBooleanType(types[5])) {
                    Object result = method.invoke(animationHandler,
                            numberValue(types[0], priority),
                            animation,
                            numberValue(types[2], 0.0D),
                            numberValue(types[3], 0.0D),
                            numberValue(types[4], 1.0D),
                            true);
                    if (method.getReturnType() == void.class || result != null) {
                        return true;
                    }
                    continue;
                }
                if (types.length == 5 && types[0] == String.class && types[1] == double.class && types[2] == double.class && types[3] == double.class && types[4] == boolean.class) {
                    Object result = method.invoke(animationHandler, animation, 0.0D, 0.0D, 1.0D, true);
                    if (method.getReturnType() == void.class || result != null) {
                        return true;
                    }
                    continue;
                }
                if (types.length == 5 && types[0] == String.class && types[1] == int.class && types[2] == int.class && types[3] == double.class && types[4] == boolean.class) {
                    Object result = method.invoke(animationHandler, animation, 0, 0, 1.0D, true);
                    if (method.getReturnType() == void.class || result != null) {
                        return true;
                    }
                    continue;
                }
                if (types.length == 2 && types[0] == String.class && types[1] == boolean.class) {
                    Object result = method.invoke(animationHandler, animation, loop);
                    if (method.getReturnType() == void.class || result != null) {
                        return true;
                    }
                    continue;
                }
                if (types.length == 1 && types[0] == String.class) {
                    Object result = method.invoke(animationHandler, animation);
                    if (method.getReturnType() == void.class || result != null) {
                        return true;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                if (!animationDiagnosticsLogged) {
                    plugin.getLogger().warning("ModelEngine rejected animation '" + animation + "' via "
                            + method.toGenericString() + ": " + rootMessage(ignored));
                }
            }
        }
        return false;
    }

    private boolean usesPriorityStateMachine(Object animationHandler) {
        for (Method method : animationHandler.getClass().getMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (method.getName().equals("playAnimation")
                    && types.length == 6
                    && isIntegerType(types[0])
                    && types[1] == String.class) {
                return true;
            }
        }
        return false;
    }

    private void logAnimationDiagnostics(Object animationHandler, List<String> animations) {
        if (animationDiagnosticsLogged) {
            return;
        }
        animationDiagnosticsLogged = true;
        plugin.getLogger().warning("ModelEngine could not start any animation from " + animations
                + ". Handler: " + animationHandler.getClass().getName());
        for (Method method : animationHandler.getClass().getMethods()) {
            if (method.getName().equals("playAnimation")) {
                plugin.getLogger().warning("Detected ModelEngine method: " + method.toGenericString());
            }
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return current.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private boolean isIntegerType(Class<?> type) {
        return type == int.class || type == Integer.class || type == long.class || type == Long.class;
    }

    private boolean isNumberType(Class<?> type) {
        return type == double.class || type == Double.class
                || type == float.class || type == Float.class
                || isIntegerType(type);
    }

    private boolean isBooleanType(Class<?> type) {
        return type == boolean.class || type == Boolean.class;
    }

    private Object numberValue(Class<?> type, double value) {
        if (type == float.class || type == Float.class) {
            return (float) value;
        }
        if (type == int.class || type == Integer.class) {
            return (int) value;
        }
        if (type == long.class || type == Long.class) {
            return (long) value;
        }
        return value;
    }

    private boolean invokeAddModel(Object modeledEntity, Object activeModel) {
        Method best = findAddModelMethod(modeledEntity.getClass(), activeModel.getClass());
        if (best == null) {
            return false;
        }
        try {
            Class<?>[] parameters = best.getParameterTypes();
            if (parameters.length == 1) {
                best.invoke(modeledEntity, activeModel);
                return true;
            }
            if (parameters.length == 2 && isBooleanType(parameters[1])) {
                best.invoke(modeledEntity, activeModel, true);
                return true;
            }
            if (parameters.length == 2 && parameters[1] == String.class) {
                best.invoke(modeledEntity, activeModel, "");
                return true;
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            warn("ModelEngine addModel failed: " + rootMessage(exception));
        }
        return false;
    }

    private Method findAddModelMethod(Class<?> modeledEntityClass, Class<?> activeModelClass) {
        for (Method method : modeledEntityClass.getMethods()) {
            if (!method.getName().equals("addModel")) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && parameters[0].isAssignableFrom(activeModelClass)) {
                return method;
            }
            if (parameters.length == 2 && parameters[0].isAssignableFrom(activeModelClass)
                    && (isBooleanType(parameters[1]) || parameters[1] == String.class)) {
                return method;
            }
        }
        return null;
    }

    private void collectModelIdsFromApi(Set<String> ids) {
        try {
            Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            for (Method method : api.getMethods()) {
                if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
                    continue;
                }
                String name = method.getName().toLowerCase();
                if (!name.contains("model") && !name.contains("blueprint") && !name.contains("registry")) {
                    continue;
                }
                try {
                    Object value = method.invoke(null);
                    collectStrings(value, ids, 4, Collections.newSetFromMap(new IdentityHashMap<>()));
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
    }

    private void collectAnimationIdsFromApi(String modelId, Set<String> ids) {
        try {
            Class<?> api = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Object activeModel = api.getMethod("createActiveModel", String.class).invoke(null, modelId);
            collectStrings(activeModel, ids, 6, Collections.newSetFromMap(new IdentityHashMap<>()));
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        ids.removeIf(value -> !looksLikeAnimation(value));
    }

    private void collectStrings(Object value, Set<String> output, int depth, Set<Object> seen) {
        if (value == null || depth <= 0) {
            return;
        }
        if (value instanceof String string) {
            if (looksLikeId(string)) {
                output.add(string);
            }
            return;
        }
        if (!seen.add(value)) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Object key : map.keySet()) {
                collectStrings(key, output, depth - 1, seen);
            }
            for (Object child : map.values()) {
                collectStrings(child, output, depth - 1, seen);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object child : collection) {
                collectStrings(child, output, depth - 1, seen);
            }
            return;
        }
        for (Method method : value.getClass().getMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
                continue;
            }
            String name = method.getName().toLowerCase();
            if (name.equals("getclass") || (!name.contains("id") && !name.contains("name") && !name.contains("model") && !name.contains("animation") && !name.contains("blueprint"))) {
                continue;
            }
            try {
                collectStrings(method.invoke(value), output, depth - 1, seen);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private void collectModelIdsFromFiles(Set<String> ids) {
        for (File file : modelEngineFiles()) {
            String name = file.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
            if (looksLikeId(name)) {
                ids.add(name);
            }
        }
    }

    private void collectAnimationIdsFromFiles(String modelId, Set<String> ids) {
        Pattern quotedName = Pattern.compile("\"(?:name|animation|id)\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Pattern yamlName = Pattern.compile("(?im)^\\s*(?:name|animation|id)\\s*:\\s*['\"]?([a-zA-Z0-9_.:-]+)");
        for (File file : modelEngineFiles()) {
            if (modelId != null && !modelId.isBlank() && !file.getName().toLowerCase().contains(modelId.toLowerCase())) {
                String fileText = readSmallFile(file);
                if (fileText == null || !fileText.toLowerCase().contains(modelId.toLowerCase())) {
                    continue;
                }
                collectAnimationMatches(fileText, ids, quotedName);
                collectAnimationMatches(fileText, ids, yamlName);
                continue;
            }
            String text = readSmallFile(file);
            if (text == null) {
                continue;
            }
            collectAnimationMatches(text, ids, quotedName);
            collectAnimationMatches(text, ids, yamlName);
        }
        ids.removeIf(value -> !looksLikeAnimation(value));
    }

    private void collectAnimationMatches(String text, Set<String> ids, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (looksLikeAnimation(value)) {
                ids.add(value);
            }
        }
    }

    private List<File> modelEngineFiles() {
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File folder = new File(pluginsFolder, "ModelEngine");
        List<File> files = new ArrayList<>();
        collectModelEngineFiles(folder, files);
        return files;
    }

    private void collectModelEngineFiles(File folder, List<File> files) {
        File[] children = folder == null ? null : folder.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectModelEngineFiles(child, files);
                continue;
            }
            String name = child.getName().toLowerCase();
            if (name.endsWith(".bbmodel") || name.endsWith(".json") || name.endsWith(".yml") || name.endsWith(".yaml")) {
                files.add(child);
            }
        }
    }

    private String readSmallFile(File file) {
        try {
            if (file.length() > 2_000_000L) {
                return null;
            }
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean looksLikeId(String value) {
        return value != null && value.length() <= 64 && value.matches("[a-zA-Z0-9_.:-]+");
    }

    private boolean looksLikeAnimation(String value) {
        if (!looksLikeId(value)) {
            return false;
        }
        String normalized = value.toLowerCase();
        return normalized.contains("open")
                || normalized.contains("close")
                || normalized.contains("idle")
                || normalized.contains("anim")
                || normalized.contains("rare")
                || normalized.contains("mouve")
                || normalized.contains("move")
                || normalized.contains("loop");
    }

    private List<String> sorted(Set<String> values) {
        return values.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private void warn(String message) {
        if (warned) {
            return;
        }
        warned = true;
        plugin.getLogger().warning(message);
    }

    public record ModelHandle(UUID entityId, String modelId, Object modeledEntity, Object activeModel) {
    }
}
