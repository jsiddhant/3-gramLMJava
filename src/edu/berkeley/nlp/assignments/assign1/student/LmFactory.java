package edu.berkeley.nlp.assignments.assign1.student;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import edu.berkeley.nlp.langmodel.EnglishWordIndexer;
import edu.berkeley.nlp.langmodel.LanguageModelFactory;
import edu.berkeley.nlp.langmodel.NgramLanguageModel;
import edu.berkeley.nlp.util.CollectionUtils;

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
//	private void checks(){
////		int count_bigrams = bigrams.size();
////		int fert_sum = 0;
////		long[] uni_keys = unigrams.getKeyList();
////		for(long key:uni_keys){
////			fert_sum += Math.max(unigrams.getFerts(key),0);
////		}
////
////		int count_trigrams = trigrams.size();
////		fert_sum = 0;
////
////		for(long key:bigrams.getKeyList()){
////			fert_sum += Math.max(bigrams.getFerts(key), 0);
////		}
////		int a = 1;
//		HashSet<Integer> temp = new HashSet<>();
//		int sum =0;
//		for(long k:bigrams.getKeyList()){
//			int[] words = decodeKey(k);
//			if(words[0]==3){
//				sum += bigrams.get(k);
//				temp.add(words[1]);
//			}
//		}
//
//		int the_uni_sum = unigrams.get(encodeKey(3));
//		int t = temp.size();
//		int a=1;
//	}
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
		double d = 0.7;
		if (to - from == 3) {
			// Unigram Fertility / Total Fertility (same as num Bigrams).
			double p1 = (double) Math.max(unigrams.getFerts(encodeKey(ngram[from+2])), 0) / bigrams.size();

			double alpha_p1_multiplier = Math.max(unigrams.getAlphas(encodeKey(ngram[from+1])), 0);
			alpha_p1_multiplier /= Math.max(unigrams.getFerts(encodeKey(ngram[from+1])),1);

			double alpha_p1 = unigrams.keyExists(encodeKey(ngram[from+1])) ?  d * alpha_p1_multiplier : 1;

			double p2 = Math.max(bigrams.getFerts(encodeKey(ngram[from+1], ngram[from+2]))-d, 0)
									/ Math.max(unigrams.getFerts(encodeKey(ngram[from+1])),1) + alpha_p1 * p1;
			double alpha_p2_multiplier = Math.max(bigrams.getAlphas(encodeKey(ngram[from+0], ngram[from+1])), 0);
			alpha_p2_multiplier /= Math.max(bigrams.get(encodeKey(ngram[from+0], ngram[from+1])), 1);

			double alpha_p2 = bigrams.keyExists(encodeKey(ngram[from+0], ngram[from+1])) ? d * alpha_p2_multiplier : 1;

			double p3 = Math.max(trigrams.get(encodeKey(ngram[from+0], ngram[from+1], ngram[from+2]))-d, 0)
									/ Math.max(bigrams.get(encodeKey(ngram[from+0], ngram[from+1])), 1);
			double f_value = p3 + alpha_p2 * p2;

			if(f_value == 0){
				return -1000;
			}
			f_value =  Math.log(f_value);
			return f_value;
		}
		else if(to - from == 2){
			double p1 = (double) Math.max(unigrams.getFerts(encodeKey(ngram[from+1])), 0) / bigrams.size();

			double alpha_p1_multiplier = Math.max(unigrams.getAlphas(encodeKey(ngram[from+0])), 0);
			alpha_p1_multiplier /= Math.max(unigrams.get(encodeKey(ngram[from+0])), 1);

			double alpha_p1 = unigrams.keyExists(encodeKey(ngram[from+0])) ? d * alpha_p1_multiplier : 1;

			double p2 = Math.max(bigrams.get(encodeKey(ngram[from+0], ngram[from+1]))-d, 0)
					/ Math.max(unigrams.get(encodeKey(ngram[from+0])), 1);

			double f_value = p2 + alpha_p1 * p1;
			if(f_value == 0){
				return -1000;
			}
			return Math.log(f_value);
		}
		else if(to - from == 1){
			double f_value = Math.max(unigrams.get(ngram[from+0]), 0) / unigrams.getTotal();

			if(f_value == 0){
				return -1000;
			}
			return Math.log(f_value);
		}
		else{
			return -1000;
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
