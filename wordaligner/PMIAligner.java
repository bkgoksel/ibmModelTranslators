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
public class PMIAligner implements WordAligner {

  private static final long serialVersionUID = 1315751943476440515L;
  
  // These hold word and pair probabilities.
  private Counter<String> wordTargetCounter;
  private Counter<String> wordSourceCounter;
  private CounterMap<String,String> sourceTargetCounter;

  public Alignment align(SentencePair sentencePair) {
    Alignment alignment = new Alignment();
    List<String> sourceSentence = sentencePair.getSourceWords();
    List<String> targetSentence = sentencePair.getTargetWords();
    for (int s = 0; s < sourceSentence.size(); s++) {
      String sourceWord = sourceSentence.get(s);
      double sourceProbability = wordSourceCounter.getCount(sourceWord);
      int bestTarget = 0;
      double bestScore = Double.NEGATIVE_INFINITY; // TODO: Make Sure this works
      for (int t = 0; t < targetSentence.size(); t++) {
        String targetWord = targetSentence.get(t);
        double targetProbability = wordTargetCounter.getCount(targetWord);
        double sourceTargetProbability = sourceTargetCounter.getCount(sourceWord, targetWord);
        double score = Math.log(sourceTargetProbability) - (Math.log(sourceProbability) + Math.log(targetProbability));
        if (score >= bestScore) {
          bestScore = score;
          bestTarget = t;
        }
      }
      // At this point the source word at index s is best aligned to the target word at index bestTarget.
      alignment.addGoldAlignment(bestTarget, s, true);
    }
    return alignment;
  }

  public void train(List<SentencePair> trainingPairs) {
    wordTargetCounter = new Counter<String>();
    wordSourceCounter = new Counter<String>();
    sourceTargetCounter = new CounterMap<String, String>();

    for (SentencePair pair : trainingPairs) {
      List<String> sourceSentence = pair.getSourceWords();
      List<String> targetSentence = pair.getTargetWords();
      for (String word : sourceSentence ) {
        wordSourceCounter.incrementCount(word, 1.0);
      }
      for (String word : targetSentence ) {
        wordTargetCounter.incrementCount(word, 1.0);
      }
      for (String sourceWord : sourceSentence ) {
        for (String targetWord : targetSentence ) {
          sourceTargetCounter.incrementCount(sourceWord, targetWord, 1.0);
        }
      }
    }
    Counters.normalize(wordTargetCounter); // Converts word counts to word probabilities
    Counters.normalize(wordSourceCounter);
    Counters.conditionalNormalize(sourceTargetCounter);
  }
}
