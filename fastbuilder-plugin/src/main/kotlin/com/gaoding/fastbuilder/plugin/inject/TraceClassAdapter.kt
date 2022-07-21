package com.gaoding.fastbuilder.plugin.inject;

import com.gaoding.fastbuilder.lib.utils.Log
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

class TraceClassAdapter(api: Int, cv: ClassVisitor?, var mInjectData: MatrixInjector.InjectData?) : ClassVisitor(api, cv) {

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
        if (mInjectData?.methodName != null && mInjectData?.methodName.equals(name)) {// && opcode == Opcodes.RETURN
            val methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
            return TraceMethodVisitor(methodVisitor)
        }

        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    inner class TraceMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.ASM9, mv) {
        override fun visitCode() {
            Log.i("TraceMethodVisitor  visitCode")
            mv.apply {
                visitVarInsn(Opcodes.ALOAD, 0)
                visitMethodInsn(Opcodes.INVOKESTATIC, "com/gaoding/fastbuilder/hotpatch/hack/HotPatchApplication", "init", "(Landroid/content/Context;)V", false);
            }
            super.visitCode()
        }

        override fun visitInsn(opcode: Int) {
            super.visitInsn(opcode)
        }
    }

}

