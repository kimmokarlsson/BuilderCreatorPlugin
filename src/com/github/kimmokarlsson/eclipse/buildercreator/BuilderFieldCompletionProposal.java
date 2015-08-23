package com.github.kimmokarlsson.eclipse.buildercreator;

import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.ui.text.java.correction.ASTRewriteCorrectionProposal;

public class BuilderFieldCompletionProposal extends ASTRewriteCorrectionProposal {

	private static final int RELEVANCE = 10;
	private static final String NAME = "Generate Builder Field";

	public BuilderFieldCompletionProposal(ICompilationUnit cu, FieldDeclaration fieldDefNode) {
		super(NAME, cu, createRewrite(fieldDefNode), RELEVANCE);
	}

	private static ASTRewrite createRewrite(FieldDeclaration fieldDefNode) {
		TypeDeclaration classType = (TypeDeclaration) fieldDefNode.getParent();

		AST classAst = classType.getAST();
		ASTRewrite rewrite = ASTRewrite.create(classAst);
		
		AST ast = AST.newAST(AST.JLS8);
		TypeDeclaration builder = ast.newTypeDeclaration();
		builder.setName(ast.newSimpleName("Builder"));
		List modifiers = builder.modifiers();
		modifiers.add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		modifiers.add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));

		List body = builder.bodyDeclarations();

		ASTNode targetFd = null;
		SimpleType builderType = ast.newSimpleType(ast.newSimpleName("Builder"));
		for (FieldDeclaration field : classType.getFields()) {
		
			if (targetFd == null) {
				targetFd = field;
			}
			
			body.add(createFieldDecl(ast, field.getType(), ((VariableDeclarationFragment)field.fragments().get(0)).getName().getIdentifier()));
		
			body.add(createMethod(ast, builderType, field.getType(), ((VariableDeclarationFragment)field.fragments().get(0)).getName().getIdentifier()));
		}
		
		rewrite.replace(targetFd, rewrite.createGroupNode(new ASTNode[] { targetFd, builder }), null);
		return rewrite;
	}
	
	private static MethodDeclaration createMethod(AST ast, Type ret, Type type, String fieldName) {
		
		// public Builder field(Type v)
		MethodDeclaration method = ast.newMethodDeclaration();
		method.setName(ast.newSimpleName(fieldName));
		List modifiers = method.modifiers();
		modifiers.add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		method.setReturnType2(typeCopy(ast, ret));
		List params = method.parameters();
		SingleVariableDeclaration par = ast.newSingleVariableDeclaration();
		par.setType(typeCopy(ast,type));
		par.setName(ast.newSimpleName("v"));
		params.add(par);
		Block body = ast.newBlock();
		List stmtList = body.statements();
		
		// 1) this.field = v;
		Assignment assignment = ast.newAssignment();
		FieldAccess fa = ast.newFieldAccess();
		fa.setExpression(ast.newThisExpression());
		fa.setName(ast.newSimpleName(fieldName));
		assignment.setLeftHandSide(fa);
		assignment.setRightHandSide(ast.newSimpleName("v"));
		ExpressionStatement asStmt = ast.newExpressionStatement(assignment);
		stmtList.add(asStmt);
		
		// 2) return this
		ReturnStatement retStmt = ast.newReturnStatement();
		retStmt.setExpression(ast.newThisExpression());
		stmtList.add(retStmt);
		method.setBody(body);
		return method;
	}
	
	private static FieldDeclaration createFieldDecl(AST ast, Type type, String fieldName) {
		VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
		frag.setName(ast.newSimpleName(fieldName));
		FieldDeclaration decl = ast.newFieldDeclaration(frag);
		decl.setType(typeCopy(ast, type));
		List mods = decl.modifiers();
		mods.add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		return decl;
	}

	private static Type typeCopy(AST ast, Type type) {
		if (type.isPrimitiveType()) {
			return ast.newPrimitiveType(((PrimitiveType)type).getPrimitiveTypeCode());
		}
		return ast.newSimpleType(ast.newName(((SimpleType)type).getName().getFullyQualifiedName()));
	}
}
