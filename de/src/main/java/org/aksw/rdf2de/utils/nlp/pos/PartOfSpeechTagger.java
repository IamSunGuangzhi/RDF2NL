package org.aksw.rdf2de.utils.nlp.pos;

import java.util.List;

import com.aliasi.tag.Tagging;

public interface PartOfSpeechTagger {

	String getName();

	String tag(String sentence);

	List<String> tagTopK(String sentence);

	Tagging<String> getTagging(String sentence);

}