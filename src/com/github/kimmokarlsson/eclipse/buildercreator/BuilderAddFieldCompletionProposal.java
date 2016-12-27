package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BuilderAddFieldCompletionProposal extends ASTRewriteCorrectionProposal {

	private static final int RELEVANCE = 10;
	private static final String NAME = "Generate Builder Field";

	public BuilderAddFieldCompletionProposal(ICompilationUnit cu, FieldDeclaration fieldDefNode) {
		super(NAME, cu, createRewrite(cu, fieldDefNode), RELEVANCE);
	}

	private static ASTRewrite createRewrite(ICompilationUnit cu, FieldDeclaration fieldDefNode) {
		TypeDeclaration parentClass = (TypeDeclaration) fieldDefNode.getParent();
		TypeDeclaration builderClass = null;
		for (TypeDeclaration decl : parentClass.getTypes()) {
			if (!decl.isInterface() && "Builder".equals(decl.getName().getIdentifier())) {
				builderClass = decl;
				break;
			}
		}

		if (builderClass == null) {
			ErrorLog.warn("Builder sub-class not found", null);
			return null;
		}

		// create copy of compilation unit
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setSource(cu);
		CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);

		List<String> path = findPath(builderClass);
		TypeDeclaration modifiedBuilderClass = traversePath(astRoot, path);
		if (modifiedBuilderClass == null) {
			ErrorLog.warn("Builder sub-class not found in copy", null);
			return null;
		}
		ASTRewrite rewrite = ASTRewrite.create(parentClass.getParent().getAST());
		TypeDeclaration modifiedParentClass = (TypeDeclaration) modifiedBuilderClass.getParent();
		AST ast = modifiedParentClass.getParent().getAST();

		// extract the name of the field we are adding
		final VariableDeclarationFragment fieldDeclFrag = (VariableDeclarationFragment)fieldDefNode.fragments().get(0);
		final String fieldName = fieldDeclFrag.getName().getIdentifier();

		// find field in modified class
		if (BuilderCreatorPrefs.getBoolean(BuilderCreatorPrefs.PREF_CONVERT_FIELDS)) {
			for (FieldDeclaration fd : modifiedParentClass.getFields()) {
				if (fd.fragments().get(0) instanceof VariableDeclarationFragment) {
					VariableDeclarationFragment vd = (VariableDeclarationFragment) fd.fragments().get(0);
					if (vd.getName().getIdentifier().equals(fieldName)) {
						boolean privateFound = false;
						boolean finalFound = false;
						for (Object mod : fd.modifiers()) {
							if (mod instanceof IExtendedModifier) {
								IExtendedModifier em = ((IExtendedModifier)mod);
								if (em.isModifier()) {
									Modifier m = (Modifier) em;
									if (m.isPrivate()) {
										privateFound = true;
									}
									else if (m.isFinal()) {
										finalFound = true;
									}
								}
							}
						}
						if (!privateFound) {
							if (finalFound) {
								fd.modifiers().add(fd.modifiers().size()-1, ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
							}
							else {
								fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
							}
						}
						if (!finalFound) {
							fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD));
						}
					}
				}
			}
		}

		// find parent constructor with Builder class as single parameter
		MethodDeclaration pconstr = null;
		for (MethodDeclaration m : modifiedParentClass.getMethods()) {
			if (m.isConstructor() && m.parameters().size() == 1) {
				pconstr = m;
				break;
			}
		}

		// find origin field neighbor
		int fieldIndex = 0;
		for (FieldDeclaration f : parentClass.getFields()) {
			if (f.equals(fieldDefNode)) {
				break;
			}
			fieldIndex++;
		}
		// check neighbors
		int neighborOffset = 0;
		FieldDeclaration neighborField = null;
		if (fieldIndex > 0) {
			neighborField = parentClass.getFields()[fieldIndex-1];
		}
		else {
			neighborOffset = -1;
			neighborField = parentClass.getFields()[fieldIndex+1];
		}
		String neighborName = ((VariableDeclarationFragment)neighborField.fragments().get(0)).getName().getIdentifier();

		// add parent class constructor assignment
		if (pconstr != null) {
			Assignment ass = ast.newAssignment();
			FieldAccess thisField = ast.newFieldAccess();
			thisField.setExpression(ast.newThisExpression());
			thisField.setName(ast.newSimpleName(fieldName));
			ass.setLeftHandSide(thisField);
			FieldAccess otherField = ast.newFieldAccess();
			otherField.setExpression(ast.newSimpleName("b"));
			otherField.setName(ast.newSimpleName(fieldName));
			ass.setRightHandSide(otherField);
			ExpressionStatement stmt = ast.newExpressionStatement(ass);

			int ctrIndex = 0;
			for (Object ctrBodyStmtObj : pconstr.getBody().statements()) {
				if (ctrBodyStmtObj instanceof ExpressionStatement
						&& ((ExpressionStatement)ctrBodyStmtObj).getExpression() instanceof Assignment) {
					Assignment bodyAss = (Assignment) ((ExpressionStatement)ctrBodyStmtObj).getExpression();
					if (bodyAss.getLeftHandSide() instanceof FieldAccess
							&& ((FieldAccess)bodyAss.getLeftHandSide()).getName().getIdentifier().equals(neighborName)) {
						ctrIndex++;
						break;
					}
				}
				ctrIndex++;
			}
			// insert into correct place
			pconstr.getBody().statements().add(ctrIndex+neighborOffset, stmt);
		}

		// add parent class getter
		{
			MethodDeclaration getter = ast.newMethodDeclaration();
			getter.setName(ast.newSimpleName(getGetterName(fieldName, fieldDefNode.getType())));
			getter.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			getter.setReturnType2(createTypeCopy(fieldDefNode.getType(), ast));
			Block body = ast.newBlock();
			ReturnStatement ret = ast.newReturnStatement();
			ret.setExpression(ast.newSimpleName(fieldName));
			body.statements().add(ret);
			getter.setBody(body);

			// find neighbor field getter
			boolean exists = false;
			MethodDeclaration neighborGetter = null;
			for (MethodDeclaration m : modifiedParentClass.getMethods()) {
				if (m.getName().getIdentifier().equals(getGetterName(neighborName, neighborField.getType()))) {
					neighborGetter = m;
				}
				else if (m.getName().getIdentifier().equals(getGetterName(fieldName, fieldDefNode.getType()))) {
					exists = true;
				}
			}

			int index = modifiedParentClass.bodyDeclarations().indexOf(neighborGetter);
			if (index <= 0) {
				index = modifiedParentClass.bodyDeclarations().size()-1;
			}
			if (!exists) {
				modifiedParentClass.bodyDeclarations().add(index+1+neighborOffset, getter);
			}
		}

		// find neighbor from builder
		int index = 0;
		for (FieldDeclaration f : modifiedBuilderClass.getFields()) {
			String bid = ((VariableDeclarationFragment)f.fragments().get(0)).getName().getIdentifier();
			if (neighborName.equals(bid)) {
				break;
			}
			index++;
		}

		// add builder field
		{
			VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
			frag.setName(ast.newSimpleName(fieldName));
			FieldDeclaration builderField = ast.newFieldDeclaration(frag);
			List modifiers = builderField.modifiers();
			modifiers.add(ast.newModifier(ModifierKeyword.PRIVATE_KEYWORD));
			builderField.setType(createTypeCopy(fieldDefNode.getType(), ast));
			modifiedBuilderClass.bodyDeclarations().add(index+1+neighborOffset, builderField);
		}

		// find builder constructor with parent class as single parameter
		MethodDeclaration constr = null;
		for (MethodDeclaration m : modifiedBuilderClass.getMethods()) {
			if (m.isConstructor() && m.parameters().size() == 1) {
				constr = m;
				break;
			}
		}
		// add constructor assignment
		if (constr != null) {
			Assignment ass = ast.newAssignment();
			FieldAccess thisField = ast.newFieldAccess();
			thisField.setExpression(ast.newThisExpression());
			thisField.setName(ast.newSimpleName(fieldName));
			ass.setLeftHandSide(thisField);
			FieldAccess otherField = ast.newFieldAccess();
			otherField.setExpression(ast.newSimpleName("c"));
			otherField.setName(ast.newSimpleName(fieldName));
			ass.setRightHandSide(otherField);
			ExpressionStatement stmt = ast.newExpressionStatement(ass);

			int ctrIndex = 0;
			for (Object ctrBodyStmtObj : constr.getBody().statements()) {
				if (ctrBodyStmtObj instanceof ExpressionStatement
						&& ((ExpressionStatement)ctrBodyStmtObj).getExpression() instanceof Assignment) {
					Assignment bodyAss = (Assignment) ((ExpressionStatement)ctrBodyStmtObj).getExpression();
					if (bodyAss.getLeftHandSide() instanceof FieldAccess
							&& ((FieldAccess)bodyAss.getLeftHandSide()).getName().getIdentifier().equals(neighborName)) {
						ctrIndex++;
						break;
					}
				}
				ctrIndex++;
			}
			// insert into correct place
			constr.getBody().statements().add(ctrIndex+neighborOffset, stmt);
		}

		// add setter method to builder
		String methodName;
		String methodPrefix = BuilderCreatorPrefs.getString(BuilderCreatorPrefs.PREF_BUILDER_PREFIX);
		if (methodPrefix == null || methodPrefix.length() == 0) {
			methodName = fieldName;
		}
		else {
			methodName = methodPrefix + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
		}

		MethodDeclaration setter = ast.newMethodDeclaration();
		setter.setName(ast.newSimpleName(methodName));
		setter.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		setter.setReturnType2(ast.newSimpleType(ast.newSimpleName("Builder")));
		SingleVariableDeclaration par = ast.newSingleVariableDeclaration();
		par.setType(createTypeCopy(fieldDefNode.getType(), ast));
		par.setName(ast.newSimpleName("p"));
		setter.parameters().add(par);
		Block body = ast.newBlock();
		Assignment ass = ast.newAssignment();
		FieldAccess thisField = ast.newFieldAccess();
		thisField.setExpression(ast.newThisExpression());
		thisField.setName(ast.newSimpleName(fieldName));
		ass.setLeftHandSide(thisField);
		ass.setRightHandSide(ast.newSimpleName("p"));
		ExpressionStatement stmt = ast.newExpressionStatement(ass);
		body.statements().add(stmt);
		ReturnStatement ret = ast.newReturnStatement();
		ret.setExpression(ast.newThisExpression());
		body.statements().add(ret);
		setter.setBody(body);

		MethodDeclaration neighbor = null;
		for (MethodDeclaration m : modifiedBuilderClass.getMethods()) {
			if (m.getName().getIdentifier().equals(neighborName)) {
				neighbor = m;
				break;
			}
		}

		int setterIndex = modifiedBuilderClass.bodyDeclarations().indexOf(neighbor);
		if (setterIndex <= 0) {
			setterIndex = modifiedBuilderClass.bodyDeclarations().size()-1;
		}
		modifiedBuilderClass.bodyDeclarations().add(setterIndex+1+neighborOffset, setter);

		rewrite.replace(parentClass, modifiedParentClass, null);
		return rewrite;
	}

	private static String getGetterName(String fieldName, Type type) {
		String prefix = "get";
		if (type.isPrimitiveType() && ((PrimitiveType)type).getPrimitiveTypeCode() == PrimitiveType.BOOLEAN) {
			prefix = "is";
		}
		return prefix+Character.toUpperCase(fieldName.charAt(0))+fieldName.substring(1);
	}

	private static Type createTypeCopy(Type type, AST ast) {
		Type copy = null;
		if (type.isPrimitiveType()) {
			PrimitiveType pt = (PrimitiveType) type;
			copy = ast.newPrimitiveType(pt.getPrimitiveTypeCode());
		}
		else if (type.isQualifiedType()) {
			QualifiedType qt = (QualifiedType) type;
			copy = ast.newQualifiedType(createTypeCopy(qt.getQualifier(), ast), (SimpleName)copyName(qt.getName(),ast));
		}
	    else if (type.isParameterizedType()) {
			ParameterizedType pt = (ParameterizedType) type;
			copy = ast.newParameterizedType(createTypeCopy(pt.getType(), ast));
			for (Object obj : pt.typeArguments()) {
				Type st = (Type) obj;
				((ParameterizedType)copy).typeArguments().add(createTypeCopy(st, ast));
			}
		}
	    else if (type.isSimpleType()) {
	    	SimpleType st = (SimpleType) type;
	    	copy = ast.newSimpleType(copyName(st.getName(), ast));
	    }
		return copy;
	}

	private static Name copyName(Name name, AST ast) {
		if (name.isSimpleName()) {
			return ast.newSimpleName(name.getFullyQualifiedName());
		}
		return ast.newName(name.getFullyQualifiedName());
	}

	private static TypeDeclaration traversePath(CompilationUnit astRoot, List<String> path) {
		Iterator<String> iter = path.iterator();
		String name = iter.next();
		TypeDeclaration type = null;
		for (Object obj : astRoot.types()) {
			if (obj instanceof TypeDeclaration) {
				TypeDeclaration t = (TypeDeclaration) obj;
				if (t.getName().getIdentifier().equals(name)) {
					type = t;
					break;
				}
			}
		}
		if (type == null) {
			ErrorLog.error("Type declaration somehow not found again.", new NullPointerException());
			return null;
		}
		while (iter.hasNext()) {
			name = iter.next();
			for (TypeDeclaration t : type.getTypes()) {
				if (t.getName().getIdentifier().equals(name)) {
					type = t;
					break;
				}
			}
		}
		return type;
	}

	private static List<String> findPath(TypeDeclaration orig) {
		List<String> list = new ArrayList<>();
		TypeDeclaration node = orig;
		while (node != null) {
			list.add(0, node.getName().getIdentifier());
			ASTNode parent = node.getParent();
			if (parent instanceof CompilationUnit) {
				break;
			}
			else if (parent instanceof TypeDeclaration) {
				node = (TypeDeclaration) parent;
			}
			else {
				throw new IllegalStateException("Unknown hierarchy!");
			}
		}
		return list;
	}
}
