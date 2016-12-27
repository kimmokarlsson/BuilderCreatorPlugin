package com.github.kimmokarlsson.eclipse.buildercreator;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;

public class BuilderAddFieldQuickAssistProcessor extends AbstractBuilderFieldQuickAssistProcessor {

	@Override
	protected IJavaCompletionProposal createBuilderFieldCompletionProposal(ICompilationUnit compilationUnit, FieldDeclaration fieldNode) {
		return new BuilderAddFieldCompletionProposal(compilationUnit, fieldNode);
	}
}
