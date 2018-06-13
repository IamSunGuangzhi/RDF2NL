/*
 * #%L
 * Triple2NL
 * %%
 * Copyright (C) 2015 Agile Knowledge Engineering and Semantic Web (AKSW)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.aksw.rdf2fr.triple2nl.converter;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.IllegalDateTimeFieldException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.datatypes.xsd.impl.RDFLangString;
import org.apache.jena.datatypes.xsd.impl.XSDAbstractDateTimeType;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.utilities.OwlApiJenaUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;

/**
 * A converter for literals.
 * 
 * @author Diego Moussallem
 */
public class LiteralConverterFrench {

	// private static final Locale ENGLISH_LOCAL = Locale.UK;
	private static final Locale FRENCH_LOCAL = Locale.FRENCH;
	private static final Logger logger = LoggerFactory.getLogger(LiteralConverterFrench.class);
	private IRIConverter iriConverter;
	private DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG, FRENCH_LOCAL);
	private boolean encapsulateStringLiterals = true;

	public LiteralConverterFrench(IRIConverter iriConverter) {
		this.iriConverter = iriConverter;
	}

	/**
	 * Convert the literal into natural language.
	 * 
	 * @param lit
	 *            the literal
	 * @return the natural language expression
	 */
	public String convert(OWLLiteral lit) {
		return convert(OwlApiJenaUtils.getLiteral(lit));
	}

	/**
	 * Convert the literal into natural language.
	 * 
	 * @param lit
	 *            the literal
	 * @return the natural language expression
	 */
	public String convert(Literal lit) {
		return convert(
				NodeFactory.createLiteral(lit.getLexicalForm(), lit.getLanguage(), lit.getDatatype()).getLiteral());
	}

	/**
	 * Convert the literal into natural language.
	 * 
	 * @param lit
	 *            the literal
	 * @return the natural language expression
	 */
	public String convert(LiteralLabel lit) {
		RDFDatatype dt = lit.getDatatype();

		String s = lit.getLexicalForm();
		if (dt == null || dt instanceof RDFLangString) {// plain literal, i.e.
														// omit language tag if
														// exists
			s = lit.getLexicalForm();
			s = s.replaceAll("_", " ");
			if (encapsulateStringLiterals) {
				s = '"' + s + '"';
			}
		} else {// typed literal
			if (dt instanceof XSDDatatype) {// built-in XSD datatype
				if (dt instanceof XSDAbstractDateTimeType) {// date datetypes
					s = convertDateLiteral(lit);
				} else if (encapsulateStringLiterals && dt.equals(XSDDatatype.XSDstring)) {
					s = '"' + s + '"';
				}
			} else {// user-defined datatype
				String text = iriConverter.convert(dt.getURI(), false).toLowerCase();
				String[] split = StringUtils.splitByCharacterTypeCamelCase(text.trim());
				String datatype = Joiner.on(" ").join(Arrays.asList(split).stream().filter(str -> !str.trim().isEmpty())
						.collect(Collectors.toList()));
				s = lit.getLexicalForm() + " " + datatype;
			}
		}
		return s;
	}

	private String convertDateLiteral(LiteralLabel lit) {
		RDFDatatype dt = lit.getDatatype();
		String s = lit.getLexicalForm();

		try {
			Object value = lit.getValue();
			if (value instanceof XSDDateTime) {
				Calendar calendar = ((XSDDateTime) value).asCalendar();
				if (dt.equals(XSDDatatype.XSDgMonth)) {
					s = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, FRENCH_LOCAL);
				} else if (dt.equals(XSDDatatype.XSDgMonthDay)) {
					s = calendar.get(Calendar.DAY_OF_MONTH)
							+ calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, FRENCH_LOCAL);
				} else if (dt.equals(XSDDatatype.XSDgYearMonth)) {
					s = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, FRENCH_LOCAL) + " "
							+ calendar.get(Calendar.YEAR);
				} else if (dt.equals(XSDDatatype.XSDgYear)) {
					s = "" + calendar.get(Calendar.YEAR);
				} else {
					s = calendar.get(Calendar.DAY_OF_MONTH) + " "
							+ calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, FRENCH_LOCAL) + " "
							+ calendar.get(Calendar.YEAR);
					// dateFormat.format(calendar.getTime());
				}
			}
		} catch (DatatypeFormatException | IllegalDateTimeFieldException e) {
			logger.error("Conversion of date literal " + lit + " failed. Reason: " + e.getMessage());
			// fallback
			// DateTime time = ISODateTimeFormat.dateTime
			DateTime time;
			try {
				time = ISODateTimeFormat.dateTimeParser().parseDateTime(lit.getLexicalForm());
				s = time.toString("dd MMMM YYYY", FRENCH_LOCAL);
			} catch (Exception e1) {
				try {
					time = ISODateTimeFormat.localDateParser().parseDateTime(lit.getLexicalForm());
					s = time.toString("dd MMMM YYYY", FRENCH_LOCAL);
				} catch (Exception e2) {
					e2.printStackTrace();
					time = ISODateTimeFormat.dateParser().parseDateTime(lit.getLexicalForm());
					s = time.toString("dd MMMM YYYY", FRENCH_LOCAL);
				}
			}
		}
		return s;
	}

	public boolean isPlural(LiteralLabel lit) {
		boolean singular = false;
		double value = 0;
		try {
			value = Integer.parseInt(lit.getLexicalForm());
			singular = (value == 0d);
		} catch (NumberFormatException e) {
			try {
				value = Double.parseDouble(lit.getLexicalForm());
				singular = (value == 0d);
			} catch (NumberFormatException e1) {
			}
		}
		boolean isPlural = (lit.getDatatypeURI() != null) && !(lit.getDatatype() instanceof RDFLangString)
				&& !(lit.getDatatype() instanceof XSDDatatype) && !singular;
		return isPlural;
	}

	/**
	 * Whether to encapsulate the value of string literals in ""
	 * 
	 * @param encapsulateStringLiterals
	 *            the encapsulateStringLiterals to set
	 */
	public void setEncapsulateStringLiterals(boolean encapsulateStringLiterals) {
		this.encapsulateStringLiterals = encapsulateStringLiterals;
	}

	public String getMonthName(int month) {
		return new DateFormatSymbols().getMonths()[month - 1];
	}

	public static void main(String[] args) {
		LiteralConverterFrench conv = new LiteralConverterFrench(
				new DefaultIRIConverterFrench(SparqlEndpoint.getEndpointDBpediaLiveAKSW()));
		LiteralLabel lit;// = NodeFactory.createLiteralNode("123",
							// null,"http://dbpedia.org/datatypes/squareKilometre").getLiteral();
		// System.out.println(lit);
		// System.out.println(conv.convert(lit));

		lit = NodeFactory.createLiteral("1869-06-27", null, XSDDatatype.XSDdate).getLiteral();
		System.out.println(lit + " --> " + conv.convert(lit));

		lit = NodeFactory.createLiteral("1914-01-01T00:00:00+02:00", null, XSDDatatype.XSDgYear).getLiteral();
		System.out.println(lit + " --> " + conv.convert(lit));

		lit = NodeFactory.createLiteral("--04", null, XSDDatatype.XSDgMonth).getLiteral();
		System.out.println(lit + " --> " + conv.convert(lit));
		
		lit = NodeFactory.createLiteral("1879-03-14^^http://www.w3.org/2001/XMLSchema#date").getLiteral();
		System.out.println(lit + " --> " + conv.convert(lit));

	}
}