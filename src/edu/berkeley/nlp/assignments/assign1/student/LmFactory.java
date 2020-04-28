package edu.berkeley.nlp.assignments.assign1.student;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.berkeley.nlp.langmodel.EnglishWordIndexer;
import edu.berkeley.nlp.langmodel.LanguageModelFactory;
import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.util.CollectionUtils;
import edu.berkeley.nlp.util.StringIndexer;

public class LmFactory implements LanguageModelFactory, NgramLanguageModel
{

  /**
   * Returns a new NgramLanguageModel; this should be an instance of a class that you implement.
   * Please see edu.berkeley.nlp.langmodel.NgramLanguageModel for the interface specification.
   * 
   * @param trainingData
   */

	public NgramLanguageModel newLanguageModel(Iterable<List<String>> trainingData) {
		 return new LmFactory(trainingData); // TODO Construct an exact LM implementation here.

	}


	private static final String STOP = NgramLanguageModel.STOP;
	private static final String START = NgramLanguageModel.START;

	private final long u_mask;
	private final long b_mask;
	private final long t_mask;

	private LongKeyHashWrapper unigrams = new LongKeyHashWrapper(1000, 0.8);
	private LongKeyHashWrapper bigrams = new LongKeyHashWrapper(10000, 0.8);
	private LongKeyHashWrapper trigrams = new LongKeyHashWrapper(100000, 0.8);

	private long encodeKey(int w1, int w2, int w3 ){
		long val = 0;
		val = (((long)w3)<<40) | (((long)w2)<<20) | (((long)w1));
		return val;
	}

	private long encodeKey(int w1){
		return encodeKey(w1, 0);
	}

	private long encodeKey(int w1, int w2){
		return encodeKey(w1, w2, 0);
	}

	private int[] decodeKey(long k){
		int[] words = new int[3];
		words[0] = (int) (k & u_mask);
		words[1] = (int) ((k & b_mask)>>20);
		words[2] = (int) ((k & t_mask)>>40);

		return words;
	}
	public LmFactory(){
		u_mask = 0;
		b_mask = 0;
		t_mask = 0;
	}

	public LmFactory(Iterable<List<String>> sentenceCollection) {
		System.out.println("Building LmFactory . . .");

		long mask = ~0;
		u_mask = ~(mask<<20);
		b_mask = ~(mask<<40 | u_mask);
		t_mask = ~(mask<<60 | b_mask | u_mask);

		int sent = 0;
		for (List<String> sentence : sentenceCollection) {
			sent++;
			if (sent % 1000000 == 0) System.out.println("On sentence " + sent);
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			stoppedSentence.add(0, START);
			stoppedSentence.add(STOP);

			for(int i=0; i<stoppedSentence.size(); i++){
				if(i+2<stoppedSentence.size()){
					processTrigrams(stoppedSentence.get(i), stoppedSentence.get(i+1), stoppedSentence.get(i+2));
				}
				if(i+1<stoppedSentence.size()) {
					processBigrams(stoppedSentence.get(i), stoppedSentence.get(i + 1));
				}

				processUnigrams(stoppedSentence.get(i));
			}
		}
		buildFertsAndAlphas();
//		calculatePerplexity(sentenceCollection);
//		checks();
		System.out.println("Done building LmFactory");
		int unigramsTotal = unigrams.getTotal();
		int unigramsKeyCount = unigrams.getKeyCount();

		int bigramsTotal = bigrams.getTotal();
		int bigramsKeyCount = bigrams.getKeyCount();

		int trigramsTotal = trigrams.getTotal();
		int trigramsKeyCount = trigrams.getKeyCount();

		System.out.println("Total Unigrams seen: " + Integer.toString(unigramsTotal));
		System.out.println("Total Unique Unigrams: " + Integer.toString(unigramsKeyCount));

		System.out.println("Total Bigrams seen: " + Integer.toString(bigramsTotal));
		System.out.println("Total Unique Bigrams: " + Integer.toString(bigramsKeyCount));

		System.out.println("Total Trigrams seen: " + Integer.toString(trigramsTotal));
		System.out.println("Total Unique Trigrams: " + Integer.toString(trigramsKeyCount));
	}

	private void processUnigrams(String w1){
		long k1 = encodeKey(EnglishWordIndexer.getIndexer().addAndGetIndex(w1));
		unigrams.incrementAt(k1);
	}

	private void processBigrams(String w1, String w2){
		int w1_idx = EnglishWordIndexer.getIndexer().addAndGetIndex(w1);
		int w2_idx = EnglishWordIndexer.getIndexer().addAndGetIndex(w2);
		long k2 = encodeKey(w1_idx, w2_idx);
		bigrams.incrementAt(k2);
	}

	private void processTrigrams(String w1, String w2, String w3){
		int w1_idx = EnglishWordIndexer.getIndexer().addAndGetIndex(w1);
		int w2_idx = EnglishWordIndexer.getIndexer().addAndGetIndex(w2);
		int w3_idx = EnglishWordIndexer.getIndexer().addAndGetIndex(w3);
		long k3 = encodeKey(w1_idx, w2_idx, w3_idx);
		trigrams.incrementAt(k3);
	}

	private void calculatePerplexity(Iterable<List<String>> sentenceCollection){
		int wordCount = 0;
		double sumLogProb = 0;
		int sent = 0;
		for (List<String> sentence : sentenceCollection) {
			sent++;
			if (sent % 1000000 == 0) System.out.println("On sentence " + sent);
			List<String> stoppedSentence = new ArrayList<String>(sentence);
			wordCount += stoppedSentence.size()+1;
			stoppedSentence.add(0, START);
			stoppedSentence.add(0, START);
			stoppedSentence.add(STOP);

			sumLogProb += calculateSentenceLogProbs(stoppedSentence);
		}

		double perplexity = Math.pow(2, -1*sumLogProb/wordCount);

		System.out.println("--------------------------------------------------");
		System.out.println("-----  SumLog Prob = " + Double.toString(sumLogProb) + " -----");
		System.out.println("-------------------------------------------------");

		System.out.println("--------------------------------------------------");
		System.out.println("-----  Word Count = " + Integer.toString(wordCount) + " -----");
		System.out.println("--------------------------------------------------");

		System.out.println("--------------------------------------------------");
		System.out.println("-----  Perplexity = " + Double.toString(perplexity) + " -----");
		System.out.println("--------------------------------------------------");
	}

	private double calculateSentenceLogProbs(List<String> stoppedSentence){
		double sentLogProb = 0;
		StringIndexer st = EnglishWordIndexer.getIndexer();

		for(int i=2; i<stoppedSentence.size(); i++){
				int[] ngram = new int[]{st.addAndGetIndex(stoppedSentence.get(i-2)),
											st.addAndGetIndex(stoppedSentence.get(i-1)),
												st.addAndGetIndex(stoppedSentence.get(i))};

				sentLogProb += getNgramLogProbability(ngram, 0, 3) / Math.log(2);
		}

		return sentLogProb;
	}

	private void buildFertsAndAlphas(){
		long[] trigramKeys = trigrams.getKeyList();

		for(long key:trigramKeys){
			if(key != -1){
				int[] words = decodeKey(key);
				if(!bigrams.incrementFerts(encodeKey(words[1], words[2]))){
					throw new Error("Failed to increment fertility of Bigrams");
				}
				if(!bigrams.incrementAlpha(encodeKey(words[0], words[1]))){
					throw new Error("Failed to increment alpha of Bigrams");
				}
				/** Trial Stuff Here **/
				if(!unigrams.incrementAtemp(encodeKey(words[1]))){
					throw new Error("Atemp Increment Failed");
				}
			}
		}
		System.out.println("Processed Trigrams Keys!");

		long [] bigramKeys = bigrams.getKeyList();

		for(long key:bigramKeys){
			if(key != -1){
				int[] words = decodeKey(key);
				if(!unigrams.incrementFerts(encodeKey(words[1]))){
					throw new Error("Failed to increment Fertility at Unigrams");
				}
				if(!unigrams.incrementAlpha(encodeKey(words[0]))){
					throw new Error("Failed to increment alpha of Unigrams");
				}
			}
		}
	}

	public int getOrder(){
		return 3;
	}

	public double getNgramLogProbability(int[] ngram, int from, int to){
		double d = 1;
		if (to - from == 3) {
			// Unigram Fertility / Total Fertility (same as num Bigrams).
			double p1 = (double) Math.max(unigrams.getFerts(encodeKey(ngram[from+2])), 0) / bigrams.size();

			double alpha_p1_multiplier = Math.max(unigrams.getAlphas(encodeKey(ngram[from+1])), 0);
			alpha_p1_multiplier /= Math.max(unigrams.getAtemp(encodeKey(ngram[from+1])),1);

			double alpha_p1 = unigrams.getAtemp(encodeKey(ngram[from+1])) > 0 ?  d * alpha_p1_multiplier : 1;
			double p2 = Math.max(bigrams.getFerts(encodeKey(ngram[from+1], ngram[from+2]))-d, 0)
									/ Math.max(unigrams.getAtemp(encodeKey(ngram[from+1])),1) + alpha_p1 * p1;
			double alpha_p2_multiplier = Math.max(bigrams.getAlphas(encodeKey(ngram[from], ngram[from+1])), 0);
			alpha_p2_multiplier /= Math.max(bigrams.get(encodeKey(ngram[from], ngram[from+1])), 1);

			double alpha_p2 = bigrams.keyExists(encodeKey(ngram[from], ngram[from+1])) ? d * alpha_p2_multiplier : 1;

			double p3 = Math.max(trigrams.get(encodeKey(ngram[from], ngram[from+1], ngram[from+2]))-d, 0)
									/ Math.max(bigrams.get(encodeKey(ngram[from], ngram[from+1])), 1);
			double f_value = p3 + alpha_p2 * p2;

			if(f_value == 0){
				return -10000;
			}
			f_value =  Math.log(f_value);
			return f_value;
		}
		else if(to - from == 2){
			double p1 = (double) Math.max(unigrams.getFerts(encodeKey(ngram[from+1])), 0) / bigrams.size();

			double alpha_p1_multiplier = Math.max(unigrams.getAlphas(encodeKey(ngram[from])), 0);
			alpha_p1_multiplier /= Math.max(unigrams.get(encodeKey(ngram[from])), 1);

			double alpha_p1 = unigrams.keyExists(encodeKey(ngram[from])) ? d * alpha_p1_multiplier : 1;

			double p2 = Math.max(bigrams.get(encodeKey(ngram[from], ngram[from+1]))-d, 0)
					/ Math.max(unigrams.get(encodeKey(ngram[from])), 1);

			double f_value = p2 + alpha_p1 * p1;
			if(f_value == 0){
				return -10000;
			}
			return Math.log(f_value);
		}
		else if(to - from == 1){
			double f_value = Math.max(unigrams.get(ngram[from]), 0) / unigrams.getTotal();

			if(f_value == 0){
				return -100000;
			}
			return Math.log(f_value);
		}
		else{
			return -100000;
		}
	}

	public long getCount(int[] ngram){
		if (ngram.length > 3) return 0;

		if (ngram.length>2) {
			long key = encodeKey(ngram[0],ngram[1], ngram[2]);
			int count = trigrams.get(key);
			return count == -1 ? 0 : count;
		}

		if(ngram.length>1){
			long key = encodeKey(ngram[0], ngram[1]);
			int count = bigrams.get(key);
			return count == -1 ? 0 : count;
		}

		if(ngram.length>0){
			long key = encodeKey(ngram[0]);
			int count = unigrams.get(key);
			return count == -1 ? 0 : count;
		}
		return 0;
	}
}
