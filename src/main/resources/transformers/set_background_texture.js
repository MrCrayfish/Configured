function initializeCoreMod() {
	return {
	    'set_background_texture_1': {
	        'target': {
	            'type': 'METHOD',
	            'class': 'net.minecraft.client.gui.widget.list.AbstractList',
	            'methodName': 'func_230430_a_',
	            'methodDesc': '(Lcom/mojang/blaze3d/matrix/MatrixStack;IIF)V'
	        },
	        'transformer': patchTextureHook
	    }
	};
}

var ASMAPI = Java.type('net.minecraftforge.coremod.api.ASMAPI');
var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode');
var VarInsnNode = Java.type('org.objectweb.asm.tree.VarInsnNode');

function patchTextureHook(method) {
    var firstBindTexture = ASMAPI.findFirstMethodCall(method, ASMAPI.MethodType.VIRTUAL, "net/minecraft/client/renderer/texture/TextureManager", ASMAPI.mapMethod("func_110577_a"), "(Lnet/minecraft/util/ResourceLocation;)V");
    method.instructions.insert(firstBindTexture, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/configured/client/ClientHandler", "updateAbstractListTexture", "(Lnet/minecraft/client/gui/widget/list/AbstractList;)V", false))
    method.instructions.insert(firstBindTexture, new VarInsnNode(Opcodes.ALOAD, 0));
    var startIndex = method.instructions.indexOf(firstBindTexture) + 1;
    var secondBindTexture = ASMAPI.findFirstMethodCallAfter(method, ASMAPI.MethodType.VIRTUAL, "net/minecraft/client/renderer/texture/TextureManager", ASMAPI.mapMethod("func_110577_a"), "(Lnet/minecraft/util/ResourceLocation;)V", startIndex);
    method.instructions.insert(secondBindTexture, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/mrcrayfish/configured/client/ClientHandler", "updateAbstractListTexture", "(Lnet/minecraft/client/gui/widget/list/AbstractList;)V", false))
    method.instructions.insert(secondBindTexture, new VarInsnNode(Opcodes.ALOAD, 0));
    return method;
}