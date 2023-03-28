function initializeCoreMod() {
	return {
	    'set_background_texture_1': {
	        'target': {
	            'type': 'METHOD',
	            'class': 'net.minecraft.client.gui.components.AbstractSelectionList',
	            'methodName': 'm_86412_',
	            'methodDesc': '(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V'
	        },
	        'transformer': patchAbstractList
	    },
	    'set_background_texture_2': {
            'target': {
                'type': 'METHOD',
                'class': 'net.minecraft.client.gui.screens.Screen',
                'methodName': 'm_264065_',
                'methodDesc': '(Lcom/mojang/blaze3d/vertex/PoseStack;)V'
            },
            'transformer': patchScreen
        }
	};
}

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function patchAbstractList(method) {
    var firstBindTexture = ASMAPI.findFirstMethodCall(method, ASMAPI.MethodType.STATIC, "com/mojang/blaze3d/systems/RenderSystem", ASMAPI.mapMethod("m_157456_"), "(ILnet/minecraft/resources/ResourceLocation;)V");
    method.instructions.insert(firstBindTexture, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/configured/client/ClientHandler", "updateAbstractListTexture", "(Lnet/minecraft/client/gui/components/AbstractSelectionList;)V", false));
    method.instructions.insert(firstBindTexture, new VarInsnNode(Opcodes.ALOAD, 0));
    var startIndex = method.instructions.indexOf(firstBindTexture) + 1;
    var secondBindTexture = ASMAPI.findFirstMethodCallAfter(method, ASMAPI.MethodType.STATIC, "com/mojang/blaze3d/systems/RenderSystem", ASMAPI.mapMethod("m_157456_"), "(ILnet/minecraft/resources/ResourceLocation;)V", startIndex);
    method.instructions.insert(secondBindTexture, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/configured/client/ClientHandler", "updateAbstractListTexture", "(Lnet/minecraft/client/gui/components/AbstractSelectionList;)V", false));
    method.instructions.insert(secondBindTexture, new VarInsnNode(Opcodes.ALOAD, 0));
    return method;
}

function patchScreen(method) {
    var firstBindTexture = ASMAPI.findFirstMethodCall(method, ASMAPI.MethodType.STATIC, "com/mojang/blaze3d/systems/RenderSystem", ASMAPI.mapMethod("m_157456_"), "(ILnet/minecraft/resources/ResourceLocation;)V");
    method.instructions.insert(firstBindTexture, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/configured/client/ClientHandler", "updateScreenTexture", "(Lnet/minecraft/client/gui/screens/Screen;)V", false));
    method.instructions.insert(firstBindTexture, new VarInsnNode(Opcodes.ALOAD, 0));
    return method;
}