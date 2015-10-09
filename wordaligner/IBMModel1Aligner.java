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
public class IBMModel1Aligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  private static final double CONVERGENCE = 0.00000000001;
  private static final int MAX_ITERS = 50;
  
  // TODO: Use arrays or Counters for collecting sufficient statistics
  // from the training data.
  private CounterMap<String,String> sourceTargetCounts;

  public Alignment align(SentencePair sentencePair) {
    // Placeholder code below. 
    // TODO Implement an inference algorithm for Eq.1 in the assignment
    // handout to predict alignments based on the counts you collected with train().
    Alignment alignment = new Alignment();
    List<String> sourceSentence = sentencePair.getSourceWords();
    List<String> targetSentence = sentencePair.getTargetWords();
    for (int s = 0; s < sourceSentence.size(); s++) {
      String sourceWord = sourceSentence.get(s);
      double bestScore = Double.NEGATIVE_INFINITY;
      int bestAlign = -1;
      for (int t = 0; t < targetSentence.size(); t++) {
        String targetWord = targetSentence.get(t);
        double score = sourceTargetCounts.getCount(sourceWord, targetWord);
        if (score > bestScore) {
          bestScore = score;
          bestAlign = t;
        }
      }

      //System.out.println("Best Score: " + bestScore);
      //System.out.println("Null score: " + sourceTargetCounts.getCount(sourceWord, WordAligner.NULL_WORD));
      if (sourceTargetCounts.getCount(sourceWord, WordAligner.NULL_WORD) < bestScore) { //check to see if NULL_WORD is the best alignment
        System.out.println("French: " + sourceSentence.get(s) + "\tEnglish: " + targetSentence.get(bestAlign));
        alignment.addPredictedAlignment(bestAlign, s); //we only add alignment if it is not best aligned to null word
      }
    }
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    //initialize and perform 1st round of EM. 
    sourceTargetCounts = new CounterMap<String, String>();
    for (SentencePair pair : trainingPairs) {
      List<String> sourceSentence = pair.getSourceWords();
      List<String> targetSentence = pair.getTargetWords();
      targetSentence.add(WordAligner.NULL_WORD); //add null word 
      for (String sourceWord : sourceSentence) {
        for (String targetWord : targetSentence) {
          sourceTargetCounts.setCount(sourceWord, targetWord, 0.0001);
        }
      }
    }
    double delta = Double.POSITIVE_INFINITY;

    for (int i = 0; i < MAX_ITERS  && delta > CONVERGENCE ; i++) {
      CounterMap<String, String> tempSourceTargetCounts = new CounterMap<String, String>();
      Counter<String> targetCounts = new Counter<String>();
      delta = 0.0;
      for (SentencePair pair : trainingPairs) { //loop through all sentence pairs again
        List<String> sourceSentence = pair.getSourceWords();
        List<String> targetSentence = pair.getTargetWords();
        Counter<String> sourceTotalProb = new Counter<String>();
        for (String sourceWord : sourceSentence) { 
          for (String targetWord : targetSentence) {
            sourceTotalProb.incrementCount(sourceWord, sourceTargetCounts.getCount(sourceWord, targetWord));
          }
        }
        for (String sourceWord : sourceSentence) { //collect fractional counts c(s,t) += t(s|t)
          for (String targetWord : targetSentence) {
            double transProb = sourceTargetCounts.getCount(sourceWord, targetWord)/sourceTotalProb.getCount(sourceWord);
            tempSourceTargetCounts.incrementCount(sourceWord, targetWord, transProb);
            targetCounts.incrementCount(targetWord, transProb); //c(t) += t(s|t)
          }
        }
      }
      for (String sourceWord : tempSourceTargetCounts.keySet()) { //compute t(s|t) = c(s,t)/c(t)
        for (String targetWord : tempSourceTargetCounts.getCounter(sourceWord).keySet()) {
          double oldScore = sourceTargetCounts.getCount(sourceWord, targetWord);
          double newScore = tempSourceTargetCounts.getCount(sourceWord, targetWord)/targetCounts.getCount(targetWord);
          sourceTargetCounts.setCount(sourceWord, targetWord, newScore);
          delta += Math.pow(oldScore - newScore, 2.0);
        }
      }
      delta /= sourceTargetCounts.totalSize();
      System.out.println(delta);
    }
  }
}
