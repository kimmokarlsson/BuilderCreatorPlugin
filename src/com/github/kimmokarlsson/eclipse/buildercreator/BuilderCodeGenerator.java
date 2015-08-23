package com.github.kimmokarlsson.eclipse.buildercreator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

public class BuilderCodeGenerator {

	public static class Settings {
		private final boolean convertFieldsFinal;
		private final boolean builderFromMethod;
		private final String buildMethodName;
		private final String methodPrefix;
		
		Settings(Builder b) {
			this.convertFieldsFinal = b.convertFieldsFinal;
			this.builderFromMethod = b.builderFromMethod;
			this.buildMethodName = checkString(b.buildMethodName, "build");
			this.methodPrefix = checkString(b.methodPrefix, "");
		}
		
		private String checkString(String s, String d) {
			if (s == null || s.length() == 0 || s.trim().length() == 0) {
				return d;
			}
			return s.trim();
		}

		public static Builder builder() {
			return new Builder();
		}

		public boolean isConvertFieldsFinal() {
			return convertFieldsFinal;
		}
		public boolean isBuilderFromMethod() {
			return builderFromMethod;
		}
		public String getBuildMethodName() {
			return buildMethodName;
		}
		public String getMethodPrefix() {
			return methodPrefix;
		}
		
		public static class Builder {
			private boolean convertFieldsFinal;
			private boolean builderFromMethod;
			private String buildMethodName;
			private String methodPrefix;
			private Builder() {}
			public Builder convertFieldsFinal(boolean s) {
				this.convertFieldsFinal = s;
				return this;
			}
			public Builder builderFromMethod(boolean s) {
				this.builderFromMethod = s;
				return this;
			}
			public Builder buildMethodName(String s) {
				this.buildMethodName = s;
				return this;
			}
			public Builder methodPrefix(String s) {
				this.methodPrefix = s;
				return this;
			}
			public Settings build() {
				return new Settings(this);
			}
		}
	}
	
	public static String convertFieldToPrivateFinal(IField f) throws JavaModelException {
		int flags = f.getFlags();
		StringBuilder sb = new StringBuilder();
		if (!Flags.isPrivate(flags)) {
			sb.append("private ");
		}
		if (!Flags.isFinal(flags)) {
			sb.append("final ");
		}
		
		return sb.toString();
	}

	public static String generate(String className, List<IField> fields, Settings settings) throws JavaModelException {
		StringBuilder sb = new StringBuilder();
		
		// private constructor using builder
		sb.append("\n    ");
		sb.append(className);
		sb.append("(Builder b) {\n");
		for (IField f : fields) {
			sb.append("        this.");
			sb.append(f.getElementName());
			sb.append(" = b.");
			sb.append(f.getElementName());
			sb.append(";\n");
		}
		sb.append("    }\n");
		
		// builder method
		sb.append("    public static Builder builder() {\n");
		sb.append("        return new Builder();\n");
		sb.append("    }\n");
		
		// builderFrom method
		if (settings.isBuilderFromMethod()) {
			sb.append("    public Builder builderFrom() {\n");
			sb.append("        return new Builder(this);\n");
			sb.append("    }\n");
		}
		
		// builder class
		sb.append("\n\n    public static class Builder {\n");
		for (IField f : fields) {
			sb.append("        private ");
			sb.append(getFieldType(f));
			sb.append(" ");
			sb.append(f.getElementName());
			sb.append(";\n");
		}
		sb.append("        private Builder() {}\n");
		sb.append("        private Builder(").append(className).append(" c) {\n");
		for (IField f : fields) {
			sb.append("            this.");
			sb.append(f.getElementName());
			sb.append(" = c.");
			sb.append(f.getElementName());
			sb.append(";\n");
		}
		sb.append("        }\n");
		// setter methods
		for (IField f : fields) {
			sb.append("        public Builder ");
			sb.append(settings.getMethodPrefix());
			sb.append(f.getElementName());
			sb.append("(");
			sb.append(getFieldType(f));
			sb.append(" s) {\n");
			sb.append("            this.");
			sb.append(f.getElementName());
			sb.append(" = s;\n");
			sb.append("            return this;\n");
			sb.append("        }\n");
		}
		// build method
		sb.append("        public ");
		sb.append(className);
		sb.append(" ");
		sb.append(settings.getBuildMethodName());
		sb.append("() {\n");
		sb.append("            return new ");
		sb.append(className);
		sb.append("(this);\n");
		sb.append("        }\n");
		sb.append("    }\n");
		return sb.toString();
	}

	public static String findFirstClassName(ICompilationUnit compilationUnit) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		if (types != null && types.length > 0) {
			return types[0].getElementName();
		}
		return null;
	}

	public static String getFieldType(IField f) throws JavaModelException {
		return Signature.getSignatureSimpleName(f.getTypeSignature());
	}
	
	public static Collection<IField> findAllFIelds(ICompilationUnit compilationUnit) throws JavaModelException {
		Collection<IField> list = new ArrayList<>();
		IType[] types = compilationUnit.getTypes();
		if (types != null && types.length > 0) {
			list.addAll(Arrays.asList(types[0].getFields()));
		}
		return list;
	}
}
