package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuilderCodeGenerator {

	public static class Settings {
		private final boolean convertFieldsFinal;
		private final boolean builderFromMethod;
		private final boolean jacksonAnnotations;
		private final String buildMethodName;
		private final String methodPrefix;
		private final boolean equalsMethod;

		Settings(Builder b) {
			this.convertFieldsFinal = b.convertFieldsFinal;
			this.builderFromMethod = b.builderFromMethod;
			this.jacksonAnnotations = b.jacksonAnnotations;
			this.buildMethodName = checkString(b.buildMethodName, "build");
			this.methodPrefix = checkString(b.methodPrefix, "");
			this.equalsMethod = b.equalsMethod;
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
		public boolean isJacksonAnnotations() {
			return jacksonAnnotations;
		}
		public String getBuildMethodName() {
			return buildMethodName;
		}
		public String getMethodPrefix() {
			return methodPrefix;
		}
		public boolean isEqualsMethod() {
		    return equalsMethod;
		}

		public static class Builder {
			private boolean convertFieldsFinal;
			private boolean builderFromMethod;
			private boolean jacksonAnnotations;
			private String buildMethodName;
			private String methodPrefix;
			private boolean equalsMethod;
			private Builder() {}
			public Builder convertFieldsFinal(boolean s) {
				this.convertFieldsFinal = s;
				return this;
			}
			public Builder builderFromMethod(boolean s) {
				this.builderFromMethod = s;
				return this;
			}
			public Builder jacksonAnnotations(boolean s) {
				this.jacksonAnnotations = s;
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
			public Builder equalsMethod(boolean s) {
			    this.equalsMethod = s;
			    return this;
			}
			public Settings build() {
				return new Settings(this);
			}
		}
	}

	private static Set<String> equalsComparables = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
				"boolean", "byte", "char", "short", "int", "long")));

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

		// getter methods in parent class
		for (IField f : fields) {
			sb.append("    public ");
			String fieldType = getFieldType(f);
			sb.append(fieldType);
			if ("boolean".equals(fieldType)) {
				sb.append(" is");
			}
			else {
				sb.append(" get");
			}
			sb.append(Character.toUpperCase(f.getElementName().charAt(0)));
			sb.append(f.getElementName().substring(1));
			sb.append("() {\n");
			sb.append("        return ");
			sb.append(f.getElementName());
			sb.append(";\n    }\n\n");
		}

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

		if (settings.isEqualsMethod()) {
			sb.append("    @Override\n");
			sb.append("    public boolean equals(Object obj) {\n");
			sb.append("        if (obj == this) {\n");
			sb.append("            return true;\n");
			sb.append("        }\n");
			sb.append("        if (obj == null || getClass() != obj.getClass()) {\n");
			sb.append("            return false;\n");
			sb.append("        }\n");
			sb.append("    ");
			sb.append(className);
			sb.append(" that = (");
			sb.append(className);
			sb.append(") obj;\n");
			sb.append("        return ");
			for (IField f : fields) {
				String typ = getFieldType(f);
				boolean iseq = isEqualsComparable(typ);
				if (!iseq) {
					sb.append("Objects.equals(");
				}
				sb.append("this.");
				sb.append(f.getElementName());
				if (iseq) {
					sb.append(" ==");
				} else {
					sb.append(',');
				}
				sb.append(" that.");
				sb.append(f.getElementName());
				if (!iseq) {
					sb.append(')');
				}
				sb.append(" && ");
			}
			if (fields.size() > 0) {
				sb.setLength(sb.length()-4);
			}
			sb.append(";\n    }\n");
			sb.append("\n\n");

			sb.append("    @Override\n");
			sb.append("    public int hashCode() {\n");
			sb.append("        return Objects.hash(\n");
			for (IField f : fields) {
				sb.append(f.getElementName());
				sb.append(", ");
			}
			if (fields.size() > 0) {
				sb.setLength(sb.length()-2);
			}
			sb.append(");\n");
			sb.append("    }\n");
		}

		sb.append("\n\n");

		// jackson annotations
		if (settings.isJacksonAnnotations()) {
			sb.append("    @JsonIgnoreProperties(ignoreUnknown=true)\n");
			sb.append("    @JsonPOJOBuilder(withPrefix=\"");
			sb.append(settings.getMethodPrefix());
			sb.append("\", buildMethodName=\"");
			sb.append(settings.getBuildMethodName());
			sb.append("\")\n");
		}

		// builder class
		sb.append("    public static class Builder {\n");
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
			if (settings.getMethodPrefix().length() > 0) {
				sb.append(settings.getMethodPrefix());
				sb.append(Character.toUpperCase(f.getElementName().charAt(0)));
				sb.append(f.getElementName().substring(1));
			}
			else {
				sb.append(f.getElementName());
			}
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

	static boolean isEqualsComparable(String typ) {
		return equalsComparables.contains(typ);
	}

	public static IType findFirstClass(ICompilationUnit compilationUnit) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		if (types != null && types.length > 0) {
			return types[0];
		}
		return null;
	}

	public static String getFieldType(IField f) throws JavaModelException {
		return Signature.getSignatureSimpleName(f.getTypeSignature());
	}

	public static Collection<IField> findAllFields(ICompilationUnit compilationUnit) throws JavaModelException {
		return findAllFields(compilationUnit, null);
	}

	public static Collection<IField> findAllFields(ICompilationUnit compilationUnit, String className) throws JavaModelException {
		IType[] types = compilationUnit.getTypes();
		IType t = findType(types, className);
		if (t != null) {
			Collection<IField> list = new ArrayList<>();
			list.addAll(Arrays.asList(t.getFields()));
			return list;
		}
		return Collections.emptyList();
	}

	public static IType findType(IType[] types, String className) throws JavaModelException {
		if (types != null) {
			if (className == null) {
				return types[0];
			}
			for (IType t : types) {
				if (t.getTypeQualifiedName().equals(className)) {
					return t;
				}
				else if (t.getTypes() != null) {
					IType s = findType(t.getTypes(), className);
					if (s != null) {
						return s;
					}
				}
			}
		}
		return null;
	}

	public static String generateClassAnnotations(String firstClassName, Settings settings) {
		if (settings.isJacksonAnnotations()) {
			StringBuilder sb = new StringBuilder();
			sb.append("@JsonInclude(JsonInclude.Include.NON_NULL)\n");
			sb.append("@JsonDeserialize(builder=");
			sb.append(firstClassName);
			sb.append(".Builder.class)\n");
			return sb.toString();
		}
		return null;
	}

	public static List<String> findAllClasses(ICompilationUnit compilationUnit) throws JavaModelException {
		List<String> list = new ArrayList<>();
		IType[] types = compilationUnit.getTypes();
		if (types != null) {
			for (IType t : types) {
				list.add(t.getTypeQualifiedName());
				list.addAll(findSubClassNames(t));
			}
		}
		return list;
	}

	private static List<String> findSubClassNames(IType top) throws JavaModelException {
		List<String> list = new ArrayList<>();
		IType[] types = top.getTypes();
		if (types != null) {
			for (IType t : types) {
				list.add(t.getTypeQualifiedName());
				list.addAll(findSubClassNames(t));
			}
		}
		return list;
	}
}
