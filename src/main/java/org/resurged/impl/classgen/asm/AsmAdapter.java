package org.resurged.impl.classgen.asm;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.resurged.jdbc.SQLRuntimeException;
import org.resurged.jdbc.Select;
import org.resurged.jdbc.Update;

public class AsmAdapter extends ClassAdapter {
	private static final String CLASS_SUFFIX = "Resurged";
	private String internalClassName = null;
	private HashMap<String, Annotation> annotations = new HashMap<String, Annotation>();
	private HashMap<String, Method> methods = new HashMap<String, Method>();
	private Class<?> template;

	public AsmAdapter(ClassVisitor classVisitor, Class<?> template) {
		super(classVisitor);
		this.template = template;

		Method[] declaredMethods = template.getDeclaredMethods();
		for (int i = 0; i < declaredMethods.length; i++) {
			String methodName = declaredMethods[i].getName() + Type.getMethodDescriptor(declaredMethods[i]);
			if(declaredMethods[i].isAnnotationPresent(Select.class)){
				annotations.put(methodName, declaredMethods[i].getAnnotation(Select.class));
				methods.put(methodName, declaredMethods[i]);
			}else if(declaredMethods[i].isAnnotationPresent(Update.class)){
				annotations.put(methodName, declaredMethods[i].getAnnotation(Update.class));
				methods.put(methodName, declaredMethods[i]);
			}
		}
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		internalClassName = name + CLASS_SUFFIX;
		
		// generate empty class
		cv.visit(version, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, internalClassName, null, "org/resurged/impl/AbstractBaseQuery", new String[] { name });

		// generate constructors
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/sql/Connection;)V", null, null);
		mv.visitCode();
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitVarInsn(Opcodes.ALOAD, 1);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/resurged/impl/AbstractBaseQuery", "<init>", "(Ljava/sql/Connection;)V");
		mv.visitInsn(Opcodes.RETURN);
		mv.visitMaxs(2, 2);
		mv.visitEnd();
		
		MethodVisitor mv2 = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljavax/sql/DataSource;)V", null, null);
		mv2.visitCode();
		mv2.visitVarInsn(Opcodes.ALOAD, 0);
		mv2.visitVarInsn(Opcodes.ALOAD, 1);
		mv2.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/resurged/impl/AbstractBaseQuery", "<init>", "(Ljavax/sql/DataSource;)V");
		mv2.visitInsn(Opcodes.RETURN);
		mv2.visitMaxs(2, 2);
		mv2.visitEnd();
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		String methodDescriptor = name + desc;
		if(!methods.containsKey(methodDescriptor))
			return null;
		
		Method method = methods.get(methodDescriptor);
		Annotation annotation = annotations.get(methodDescriptor);
		
		// generate method signature
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, name, desc, signature, exceptions);
		
		// generate method body
		mv.visitCode();
		
		//////////////////////////////////////
		//// PREPARE OPERANT STACK VALUES ////
		//////////////////////////////////////
		
		// stack.push "this"
		mv.visitVarInsn(Opcodes.ALOAD, 0);

		// stack.push interface class
		mv.visitLdcInsn(Type.getType(getTypeString(template)));

		// stack.push annotation class
		if(annotation instanceof Select)
			mv.visitLdcInsn(Type.getType("Lorg/resurged/jdbc/Select;"));
		else
			mv.visitLdcInsn(Type.getType("Lorg/resurged/jdbc/Update;"));

		// stack.push method name
		mv.visitLdcInsn(name);
		
//		if(annotation instanceof Select){
//			java.lang.reflect.Type returnType = method.getGenericReturnType();
//			if(returnType instanceof ParameterizedType){
//			    ParameterizedType type = (ParameterizedType) returnType;
//			    java.lang.reflect.Type[] typeArguments = type.getActualTypeArguments();
//			    Class<?> typeArgClass = (Class<?>) typeArguments[0];
//				// stack.push generic return type
//			    mv.visitLdcInsn(Type.getType("L" + typeArgClass.getName().replaceAll("\\.", "/") + ";"));
//			}
//		}

		// stack.push size of parameter type array
		mv.visitIntInsn(Opcodes.BIPUSH, method.getParameterTypes().length);
		
		// stack.pop array size and stack.push new array of that size
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Class");
		
		// move parameter types to array
		Class<?>[] parameterTypes = method.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			// stack.dublicate array reference
			mv.visitInsn(Opcodes.DUP);
			
			// stack.push array index
			mv.visitIntInsn(Opcodes.BIPUSH, i);
			
			// stack.push parameter type
			if(parameterTypes[i].isPrimitive())
				mv.visitFieldInsn(Opcodes.GETSTATIC, getTypeString(parameterTypes[i]), "TYPE", "Ljava/lang/Class;");
			else
				mv.visitLdcInsn(Type.getType(getTypeString(parameterTypes[i])));
			
			// stack.pop index+value and store these in duplicate array reference, which is popped
			mv.visitInsn(Opcodes.AASTORE);
		}
		
		// stack.push size of parameter value array
		mv.visitIntInsn(Opcodes.BIPUSH, method.getParameterTypes().length);
		
		// stack.pop array size and stack.push new array of that size
		mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
		
		// move parameter values to array
		int lvarIndex = 1;
		for (int i = 0; i < parameterTypes.length; i++) {
			// stack.dublicate array reference
			mv.visitInsn(Opcodes.DUP);
			
			// stack.push array index
			mv.visitIntInsn(Opcodes.BIPUSH, i);
			
			// stack.push parameter value
			lvarIndex = pushParameterValue(lvarIndex, parameterTypes[i], mv);
			
			// stack.pop index+value and store these in duplicate array reference, which is popped
			mv.visitInsn(Opcodes.AASTORE);
		}

		//////////////////////////////////////
		////        INVOKE METHOD         ////
		//////////////////////////////////////
		
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, "executeQuery", "(Ljava/lang/Class;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;");

		//////////////////////////////////////
		////        RETURN RESULT         ////
		//////////////////////////////////////
		
		if(method.getReturnType().isPrimitive()){
			mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
			mv.visitInsn(Opcodes.IRETURN);
		}else{
			mv.visitTypeInsn(Opcodes.CHECKCAST, "org/resurged/jdbc/DataSet");
			mv.visitInsn(Opcodes.ARETURN);
		}
		
		// Register local variables and frame size. 
		// Numbers are currently ignored, as this is handled by the ClassWriter
		mv.visitMaxs(0, 0);
		
		mv.visitEnd();
		return mv;
	}
	
	private int pushParameterValue(int i, Class<?> klass, MethodVisitor mv) {
		if(klass.isPrimitive()){	
			if(klass==Boolean.TYPE || klass==Byte.TYPE || klass==Character.TYPE|| klass==Short.TYPE || klass==Integer.TYPE) {
				mv.visitVarInsn(Opcodes.ILOAD, i);
			} else if(klass==Long.TYPE) {
				mv.visitVarInsn(Opcodes.LLOAD, i);
				i++;
			} else if(klass==Float.TYPE) {
				mv.visitVarInsn(Opcodes.FLOAD, i);
			} else if(klass==Double.TYPE) {
				mv.visitVarInsn(Opcodes.DLOAD, i);
				i++;
			} 
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, getTypeString(klass), "valueOf", "(" + getPrimitiveString(klass) + ")" + getWrapperString(klass));
		} else {
			mv.visitVarInsn(Opcodes.ALOAD, i);
		}
		return ++i;
	}

	public String getClassName() {
		return internalClassName.replaceAll("/", ".");
	}
	
	private String getTypeString(Class<?> klass){
		if(klass==Boolean.TYPE)
			return "java/lang/Boolean";
		else if(klass==Byte.TYPE)
			return "java/lang/Byte";
		else if(klass==Character.TYPE)
			return "java/lang/Character";
		else if(klass==Short.TYPE)
			return "java/lang/Short";
		else if(klass==Integer.TYPE)
			return "java/lang/Integer";
		else if(klass==Long.TYPE)
			return "java/lang/Long";
		else if(klass==Float.TYPE)
			return "java/lang/Float";
		else if(klass==Double.TYPE)
			return "java/lang/Double";
		else
			return "L" + klass.getName().replaceAll("\\.", "/") + ";";
	}
	
	private String getPrimitiveString(Class<?> klass){
		if(klass==Boolean.TYPE)
			return "Z";
		else if(klass==Byte.TYPE)
			return "B";
		else if(klass==Character.TYPE)
			return "C";
		else if(klass==Short.TYPE)
			return "S";
		else if(klass==Integer.TYPE)
			return "I";
		else if(klass==Long.TYPE)
			return "J";
		else if(klass==Float.TYPE)
			return "F";
		else if(klass==Double.TYPE)
			return "D";
		
		throw new SQLRuntimeException(klass.getName() + " is not a primitive");
	}
	
	private String getWrapperString(Class<?> klass){
		if(klass==Boolean.TYPE)
			return getTypeString(Boolean.class);
		else if(klass==Byte.TYPE)
			return getTypeString(Byte.class);
		else if(klass==Character.TYPE)
			return getTypeString(Character.class);
		else if(klass==Short.TYPE)
			return getTypeString(Short.class);
		else if(klass==Integer.TYPE)
			return getTypeString(Integer.class);
		else if(klass==Long.TYPE)
			return getTypeString(Long.class);
		else if(klass==Float.TYPE)
			return getTypeString(Float.class);
		else if(klass==Double.TYPE)
			return getTypeString(Double.class);
		
		throw new SQLRuntimeException(klass.getName() + " is not a primitive");
	}

}
