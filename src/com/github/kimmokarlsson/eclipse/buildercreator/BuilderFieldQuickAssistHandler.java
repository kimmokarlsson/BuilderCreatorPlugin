package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

public class BuilderFieldQuickAssistHandler implements IQuickAssistProcessor {

	public BuilderFieldQuickAssistHandler() {
	}

	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		return context != null && isClassFieldWithBuilder(context.getCoveringNode());
	}

	private FieldDeclaration findFieldNode(ASTNode node) {
		FieldDeclaration decl = null;
		if (node instanceof SimpleName && node.getParent() instanceof VariableDeclarationFragment
				&& node.getParent().getParent() instanceof FieldDeclaration) {
			decl = (FieldDeclaration) node.getParent().getParent();
		}
		else if (node instanceof VariableDeclarationFragment && node.getParent() instanceof FieldDeclaration) {
			decl = (FieldDeclaration) node.getParent();
		}
		else if (node instanceof FieldDeclaration) {
			decl = (FieldDeclaration) node;
		}
		return decl;
	}

	private boolean isClassFieldWithBuilder(ASTNode node) {
		FieldDeclaration decl = findFieldNode(node);
		return decl != null && decl.getParent() instanceof TypeDeclaration
				&& isBuilderSubClass((TypeDeclaration)decl.getParent());
	}

	private boolean isBuilderSubClass(TypeDeclaration parent) {
		if (parent.getTypes() == null) {
			return false;
		}
		for (TypeDeclaration decl : parent.getTypes()) {
			if (!decl.isInterface() && decl.getName().getIdentifier().equals("Builder")) {
				return true;
			}
		}
		return false;
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		ASTNode node = context.getCoveringNode();
		if (!isClassFieldWithBuilder(node)) {
			return new IJavaCompletionProposal[] { };
		}
		FieldDeclaration fieldNode = findFieldNode(node);
		IJavaCompletionProposal prop = new BuilderFieldCompletionProposal(context.getCompilationUnit(), fieldNode);
		return new IJavaCompletionProposal[] { prop };
	}
}
