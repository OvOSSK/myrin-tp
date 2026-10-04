package com.myrin.tp.core;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;

import java.util.List;
import java.util.Set;

/** 将选择器权限 transformer 注册进 Forge 的 modlauncher 转换链。 */
public class SelectorTransformationService implements ITransformationService {

    @Override
    public String name() {
        return "myrintp-selectors";
    }

    @Override
    public void initialize(IEnvironment environment) {
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> services) {
    }

    @Override
    public List<ITransformer> transformers() {
        return List.of(new SelectorTransformer());
    }
}
