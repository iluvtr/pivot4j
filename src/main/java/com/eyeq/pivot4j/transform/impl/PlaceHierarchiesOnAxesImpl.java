/*
 * ====================================================================
 * This software is subject to the terms of the Common Public License
 * Agreement, available at the following URL:
 *   http://www.opensource.org/licenses/cpl.html .
 * You must accept the terms of that agreement to use this software.
 * ====================================================================
 */
package com.eyeq.pivot4j.transform.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eyeq.pivot4j.PivotException;
import com.eyeq.pivot4j.mdx.Exp;
import com.eyeq.pivot4j.mdx.FunCall;
import com.eyeq.pivot4j.mdx.MemberExp;
import com.eyeq.pivot4j.mdx.Syntax;
import com.eyeq.pivot4j.query.Quax;
import com.eyeq.pivot4j.query.QueryAdapter;
import com.eyeq.pivot4j.transform.AbstractTransform;
import com.eyeq.pivot4j.transform.PlaceHierarchiesOnAxes;

public class PlaceHierarchiesOnAxesImpl extends AbstractTransform implements
		PlaceHierarchiesOnAxes {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * @param queryAdapter
	 */
	public PlaceHierarchiesOnAxesImpl(QueryAdapter queryAdapter) {
		super(queryAdapter);
	}

	/**
	 * @see com.eyeq.pivot4j.transform.PlaceHierarchiesOnAxes#placeHierarchies(org.olap4j.Axis,
	 *      java.util.List, boolean)
	 */
	public void placeHierarchies(Axis axis, List<Hierarchy> hierarchies,
			boolean expandAllMember) {
		QueryAdapter adapter = getQueryAdapter();

		// locate the appropriate query axis
		int iQuax = axis.axisOrdinal();
		if (adapter.isAxesSwapped()) {
			iQuax = (iQuax + 1) % 2;
		}

		List<Exp> memberExpressions = new ArrayList<Exp>();
		for (Hierarchy hierarchy : hierarchies) {
			memberExpressions.add(createMemberExpression(hierarchy,
					expandAllMember));
		}

		Quax quax = adapter.getQuaxes().get(iQuax);

		int nDimension = 0;
		for (Exp memberExpression : memberExpressions) {
			if (memberExpression != null) {
				++nDimension;
			}
		}

		List<Exp> sets = new ArrayList<Exp>(nDimension);

		for (Exp memberExpression : memberExpressions) {
			// null possible due to access control
			if (memberExpressions != null) {
				// object generated by createMemberExpression or
				// CalcSet.createAxisExpression
				sets.add(memberExpression);
			}
		}

		// generate the crossjoins
		quax.regeneratePosTree(sets, true);

		if (logger.isInfoEnabled()) {
			logger.info("setQueryAxis axis=" + quax.getOrdinal()
					+ " nDimension=" + nDimension);
			logger.info("Expression for Axis=" + quax.toString());
		}
	}

	/**
	 * @see com.eyeq.pivot4j.transform.PlaceHierarchiesOnAxes#findVisibleHierarchies
	 *      (org.olap4j.Axis)
	 */
	@Override
	public List<Hierarchy> findVisibleHierarchies(Axis axis) {
		QueryAdapter adapter = getQueryAdapter();

		// find the Quax for this hierarchy
		Quax quax = adapter.getQuaxes().get(axis.axisOrdinal());
		if (quax == null) {
			return Collections.emptyList(); // should not occur
		}

		return quax.getHierarchies();
	}

	/**
	 * @param hierarchy
	 * @param expandAllMember
	 * @return
	 */
	protected Exp createMemberExpression(Hierarchy hierarchy,
			boolean expandAllMember) {
		// if the query does not contain the hierarchy,
		// just return the highest level
		QueryAdapter adapter = getQueryAdapter();

		// find the Quax for this hier
		Quax quax = adapter.findQuax(hierarchy.getDimension());
		if (quax == null) {
			adapter.getCurrentMdx();
			// the hierarchy was not found on any axis
			return topLevelMembers(hierarchy, expandAllMember);
			// return top level members of the hierarchy
		}

		// the member expression is the list of members plus the list of
		// FunCalls
		// for this dimension
		int iDimension = quax.dimIdx(hierarchy.getDimension());
		return quax.genExpForDim(iDimension);
	}

	/**
	 * @param hierarchy
	 * @param expandAllMember
	 * @return
	 * @throws OlapException
	 */
	protected Exp topLevelMembers(Hierarchy hierarchy, boolean expandAllMember) {
		try {
			if (hierarchy.hasAll()) {
				// an "All" member is present -get it
				// does this call work with parent-child
				Member allMember = hierarchy.getDefaultMember();

				if (allMember == null || !allMember.isAll()) {
					allMember = null;

					List<Member> topMembers = hierarchy.getRootMembers();
					for (Member member : topMembers) {
						if (member.isAll()) {
							allMember = member;
							break;
						}
					}
				}

				if (allMember != null) {
					if (!expandAllMember) {
						return new MemberExp(allMember);
					}

					// must expand
					// create Union({AllMember}, AllMember.children)
					Exp allExp = new MemberExp(allMember);
					Exp allSet = new FunCall("{}", new Exp[] { allExp },
							Syntax.Braces);

					Exp mAllChildren = new FunCall("Children",
							new Exp[] { allExp }, Syntax.Property);
					Exp union = new FunCall("Union", new Exp[] { allSet,
							mAllChildren }, Syntax.Function);

					return union;
				}
			}

			List<Member> topMembers = hierarchy.getRootMembers();
			if (topMembers.size() == 1) {
				return new MemberExp(topMembers.get(0)); // single
															// member
			} else if (topMembers.isEmpty()) {
				return null; // possible if access control active
			}

			List<Exp> args = new ArrayList<Exp>(topMembers.size());
			for (Member member : topMembers) {
				if (member.isVisible()) {
					args.add(new MemberExp(member));
				}
			}

			return new FunCall("{}", args.toArray(new Exp[args.size()]),
					Syntax.Braces);
		} catch (OlapException e) {
			throw new PivotException(e);
		}
	}
}
