package cs224n.wordaligner;

import cs224n.util.*;
import java.util.List;

/**
 * Simple word alignment baseline model that maps source positions to target 
 * positions along the diagonal of the alignment grid.
 * 
 * IMPORTANT: Make sure that you read the comments in the
 * cs224n.wordaligner.WordAligner interface.
 * 
 * @author Dan Klein
 * @author Spence Green
 */
public class IBMModel2Aligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  private static final int MAX_ITERS = 10;
  private static final double CONVERGENCE = 0.0001;

  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
  private CounterMap<String,String> sourceTargetCounts;
  private CounterMap<Pair<Integer, Integer>, Pair<Integer, Integer>> sourceTargetDistortions;

  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
    Alignment alignment = new Alignment();
    List<String> sourceSentence = sentencePair.getSourceWords();
    List<String> targetSentence = sentencePair.getTargetWords();
    int m = sourceSentence.size();
    int l = targetSentence.size();

    Pair<Integer, Integer> lmPair = new Pair<Integer, Integer>(l, m);
    for (int i = 0; i < m; i++) {
      String sourceWord = sourceSentence.get(i);
      double bestScore = Double.NEGATIVE_INFINITY;
      int bestAlign = -1;
      for (int j = 0; j < l; j++) {
        String targetWord = targetSentence.get(j);
        Pair<Integer, Integer> jiPair = new Pair<Integer, Integer>(j, i);
        double transProb = sourceTargetCounts.getCount(sourceWord, targetWord);
        double alignProb = sourceTargetDistortions.getCount(jiPair, lmPair);
        double score = Math.log(transProb) + Math.log(alignProb);
        if (score > bestScore) {
          bestScore = score;
          bestAlign = j;
        }
      }
      if (!targetSentence.get(bestAlign).equals(WordAligner.NULL_WORD)) {
        alignment.addPredictedAlignment(bestAlign, i);
      }
    }

    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    sourceTargetCounts = new CounterMap<String, String>();
    sourceTargetDistortions = new CounterMap<Pair<Integer,Integer>, Pair<Integer, Integer>>();
    for (SentencePair pair : trainingPairs) {
      List<String> sourceSentence = pair.getSourceWords();
      List<String> targetSentence = pair.getTargetWords();
      targetSentence.add(WordAligner.NULL_WORD);
      int m = sourceSentence.size();
      int l = targetSentence.size();
      for (int i = 0; i < m; i++) {
        String sourceWord = sourceSentence.get(i);
        for (int j = 0; j < l; j++) {
          String targetWord = targetSentence.get(j);
          sourceTargetCounts.setCount(sourceWord, targetWord, 1.0);
          Pair<Integer, Integer> lmPair = new Pair<Integer,Integer>(l, m);
          Pair<Integer, Integer> jiPair = new Pair<Integer,Integer>(j, i);
          sourceTargetDistortions.setCount(jiPair, lmPair, 1.0);
        }
      }
    }

    // Use Model 1 to train params
    double delta = Double.POSITIVE_INFINITY;
    for (int i = 0; i < MAX_ITERS && delta > CONVERGENCE; i++) {
      CounterMap<String, String> tempSourceTargetCounts = new CounterMap<String, String>();
      Counter<String> targetCounts = new Counter<String>();
      delta = 0.0;
      for (SentencePair pair : trainingPairs) {
        List<String> sourceSentence = pair.getSourceWords();
        List<String> targetSentence = pair.getTargetWords();
        Counter<String> sourceTotals = new Counter<String>();

        for (String sourceWord : sourceSentence) {
          for (String targetWord : targetSentence) {
            sourceTotals.incrementCount(sourceWord, sourceTargetCounts.getCount(sourceWord, targetWord));
          }
        }
        for (String sourceWord : sourceSentence) {
          for (String targetWord : targetSentence) {
            double transProb = sourceTargetCounts.getCount(sourceWord, targetWord);
            double sourceTotal = sourceTotals.getCount(sourceWord);
            tempSourceTargetCounts.incrementCount(sourceWord, targetWord, transProb / sourceTotal);
            targetCounts.incrementCount(targetWord, transProb / sourceTotal);
          }
        }
      }

      //update t(s|t) values
      for (String sourceWord : tempSourceTargetCounts.keySet()) {
        for (String targetWord : tempSourceTargetCounts.getCounter(sourceWord).keySet()) {
          double oldProb = sourceTargetCounts.getCount(sourceWord, targetWord);
          double newProb = tempSourceTargetCounts.getCount(sourceWord, targetWord) / targetCounts.getCount(targetWord);
          sourceTargetCounts.setCount(sourceWord, targetWord, newProb);
          delta += Math.pow(oldProb - newProb, 2.0);
        }
      }
      delta /= sourceTargetCounts.totalSize();
    }

    //Maximizing for ibm model 2

    delta = Double.POSITIVE_INFINITY;
    for (int iter = 0; iter < MAX_ITERS && delta > CONVERGENCE; iter++) {
      CounterMap<String, String> tempSourceTargetCounts = new CounterMap<String, String>();
      CounterMap<Pair<Integer,Integer>,Pair<Integer, Integer>> tempSourceTargetDistortions = new CounterMap<Pair<Integer,Integer>, Pair<Integer, Integer>>();
      Counter<String> targetCounts = new Counter<String>();
      CounterMap<Pair<Integer, Integer>,Integer> targetDistorts = new CounterMap<Pair<Integer,Integer>,Integer>();
      delta = 0.0;
      for (SentencePair pair : trainingPairs) {
        List<String> sourceSentence = pair.getSourceWords();
        List<String> targetSentence = pair.getTargetWords();
        CounterMap<Pair<Integer, Integer>, Integer> distortSourceTotals = new CounterMap<Pair<Integer,Integer>,Integer>();
        Pair<Integer, Integer> lmPair = new Pair<Integer, Integer>(targetSentence.size(), sourceSentence.size());
        for (int i = 0; i < sourceSentence.size(); i++) {
          String sourceWord = sourceSentence.get(i);
          for (int j = 0; j < targetSentence.size(); j++) {
            String targetWord = targetSentence.get(j);
            Pair<Integer, Integer> jiPair = new Pair<Integer, Integer>(j, i);
            double currTransProb = sourceTargetCounts.getCount(sourceWord, targetWord);
            double currAlignProb = sourceTargetDistortions.getCount(jiPair, lmPair);
            distortSourceTotals.incrementCount(lmPair, i, currTransProb * currAlignProb);
          }
        }
        for (int i = 0; i < sourceSentence.size(); i++) {
          String sourceWord = sourceSentence.get(i);
          double distortTransSourceTotal = distortSourceTotals.getCount(lmPair, i);
          for (int j = 0; j < targetSentence.size(); j++) {
            String targetWord = targetSentence.get(j);
            Pair<Integer, Integer> jiPair = new Pair<Integer, Integer>(j, i);
            double transProb = sourceTargetCounts.getCount(sourceWord, targetWord);
            double distortProb = sourceTargetDistortions.getCount(jiPair, lmPair);
            double update = (transProb * distortProb)/(distortTransSourceTotal); //q(j|ilm)t(f|e)/totals
            tempSourceTargetCounts.incrementCount(sourceWord, targetWord, update);
            tempSourceTargetDistortions.incrementCount(jiPair, lmPair, update);
            targetCounts.incrementCount(targetWord, update);
            targetDistorts.incrementCount(lmPair, i, update);
          }
        }
      }
      //update t(s|t) values
      double delta_trans = 0.0;
      for (String sourceWord : tempSourceTargetCounts.keySet()) {
        for (String targetWord : tempSourceTargetCounts.getCounter(sourceWord).keySet()) {
          double oldProb = sourceTargetCounts.getCount(sourceWord, targetWord);
          double newProb = tempSourceTargetCounts.getCount(sourceWord, targetWord) / targetCounts.getCount(targetWord);
          sourceTargetCounts.setCount(sourceWord, targetWord, newProb);
          delta += Math.pow(oldProb - newProb, 2.0);
        }
      }
      //update q(j|ilm) values
      double delta_dist = 0.0;
      for (Pair<Integer,Integer> jiPair : tempSourceTargetDistortions.keySet()) {
        for (Pair<Integer,Integer> lmPair : tempSourceTargetDistortions.getCounter(jiPair).keySet()) {
          double oldProb = sourceTargetDistortions.getCount(jiPair, lmPair);
          double tempAlignProb = tempSourceTargetDistortions.getCount(jiPair, lmPair);
          double tempTargetDist = targetDistorts.getCount(lmPair, jiPair.getSecond());
          double newProb = tempAlignProb / tempTargetDist;
          sourceTargetDistortions.setCount(jiPair, lmPair, newProb);
          delta_dist += Math.pow(oldProb - newProb, 2.0);
        }
      }
      delta = (delta_trans / sourceTargetCounts.totalSize() + delta_dist / sourceTargetDistortions.totalSize())/2.0;
    }
  }
}
