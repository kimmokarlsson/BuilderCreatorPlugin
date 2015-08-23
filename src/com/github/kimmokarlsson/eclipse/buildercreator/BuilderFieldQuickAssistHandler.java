package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickAssistProcessor;

public class BuilderFieldQuickAssistHandler implements IQuickAssistProcessor {

	public BuilderFieldQuickAssistHandler() {
	}
	
	@Override
	public boolean hasAssists(IInvocationContext context) throws CoreException {
		return context != null && isClassField(context.getCoveredNode());
	}

	private boolean isClassField(ASTNode node) {
		return node instanceof FieldDeclaration && node.getParent() instanceof TypeDeclaration;
	}

	@Override
	public IJavaCompletionProposal[] getAssists(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		ASTNode fieldNode = context.getCoveredNode();
		if (!isClassField(fieldNode)) {
			return new IJavaCompletionProposal[] { };
		}
		IJavaCompletionProposal prop = new BuilderFieldCompletionProposal(context.getCompilationUnit(), (FieldDeclaration)fieldNode);
		return new IJavaCompletionProposal[] { prop };
	}
}
