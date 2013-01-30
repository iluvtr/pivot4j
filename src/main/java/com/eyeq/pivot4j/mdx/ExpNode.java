/*
 * ====================================================================
 * This software is subject to the terms of the Common Public License
 * Agreement, available at the following URL:
 *   http://www.opensource.org/licenses/cpl.html .
 * You must accept the terms of that agreement to use this software.
 * ====================================================================
 */
package com.eyeq.pivot4j.mdx;

import java.io.Serializable;

import com.eyeq.pivot4j.util.TreeNode;

public class ExpNode extends TreeNode<Exp> implements Serializable {

	private static final long serialVersionUID = 1956521185377274271L;

	/**
	 * @param obj
	 */
	public ExpNode(Exp exp) {
		super(exp);
	}

	/**
	 * @see com.eyeq.pivot4j.util.TreeNode#addChild(com.eyeq.pivot4j.util.TreeNode)
	 */
	@Override
	public void addChild(TreeNode<Exp> child) {
		if (!(child instanceof ExpNode)) {
			throw new IllegalArgumentException(
					"Only ExpNode instance can be added as a child node.");
		}

		super.addChild(child);
	}

	/**
	 * deep copy (clone)
	 * 
	 * @return copy of TreeNode
	 * @see com.eyeq.pivot4j.util.TreeNode#deepCopy()
	 */
	@Override
	public ExpNode deepCopy() {
		ExpNode newNode = new ExpNode(getReference());
		for (TreeNode<Exp> child : getChildren()) {
			newNode.addChild(child.deepCopy());
		}
		return newNode;
	}

	/**
	 * deep copy (clone) and prune
	 * 
	 * @param depth
	 *            - number of child levels to be copied
	 * @return copy of TreeNode
	 * @see com.eyeq.pivot4j.util.TreeNode#deepCopyPrune(int)
	 */
	@Override
	public ExpNode deepCopyPrune(int depth) {
		if (depth < 0) {
			throw new IllegalArgumentException("Depth is negative");
		}

		ExpNode newNode = new ExpNode(getReference());
		if (depth == 0) {
			return newNode;
		}

		for (TreeNode<Exp> child : getChildren()) {
			newNode.addChild(child.deepCopyPrune(depth - 1));
		}
		return newNode;
	}

	/**
	 * @see com.eyeq.pivot4j.util.TreeNode#getParent()
	 */
	@Override
	public ExpNode getParent() {
		return (ExpNode) super.getParent();
	}

	/**
	 * @see com.eyeq.pivot4j.util.TreeNode#getRoot()
	 */
	@Override
	public ExpNode getRoot() {
		return (ExpNode) super.getRoot();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getReference().toMdx();
	}
}