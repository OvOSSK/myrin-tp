var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');

function initializeCoreMod() {
    return {
        'myrintp_entity_selector': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.commands.arguments.selector.EntitySelector'
            },
            'transformer': function(classNode) {
                var methods = classNode.methods;
                for (var i = 0; i < methods.size(); i++) {
                    var method = methods.get(i);
                    if (method.name === 'm_121168_') {
                        method.instructions.clear();
                        method.instructions.add(new InsnNode(Opcodes.RETURN));
                    }
                }
                return classNode;
            }
        },
        'myrintp_entity_argument': {
            'target': {
                'type': 'CLASS',
                'name': 'net.minecraft.commands.arguments.EntityArgument'
            },
            'transformer': function(classNode) {
                var methods = classNode.methods;
                for (var i = 0; i < methods.size(); i++) {
                    var method = methods.get(i);
                    if (method.name === 'listSuggestions') {
                        var insns = method.instructions;
                        var node = insns.getFirst();
                        while (node !== null) {
                            var next = node.getNext();
                            if (node instanceof MethodInsnNode &&
                                    node.owner === 'net/minecraftforge/common/ForgeHooks' &&
                                    node.name === 'canUseEntitySelectors') {
                                insns.insertBefore(node, new InsnNode(Opcodes.POP));
                                insns.insertBefore(node, new InsnNode(Opcodes.ICONST_1));
                                insns.remove(node);
                                break;
                            }
                            node = next;
                        }
                    }
                }
                return classNode;
            }
        }
    };
}
