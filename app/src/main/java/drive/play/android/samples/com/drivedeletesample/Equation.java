/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package drive.play.android.samples.com.drivedeletesample;

import java.util.Random;

/**
 * Represents a simple math equation and its answer.
 */
class Equation {
  private String operation;
  private int p1;
  private int p2;
  private int answer;
  private Random random;

  Equation() {
    random = new Random();
    p1 = generateInteger();
    p2 = generateInteger();
    operation = pickOperation();
  }

  public String toString() {
    return p1 + " " + operation + " " + p2 + " = " + answer + " (" + checkAnswer() + ")";
  }

  /**
   * Generates a random integer.
   */
  private int generateInteger() {
    return random.nextInt(10);
  }

  /**
   * Returns the operation chosen using a RNG.
   */
  private String pickOperation() {
    int val = random.nextInt(4);
    switch (val) {
      case 0:
        return "+";
      case 1:
        return "-";
      case 2:
        return "*";
      case 3:
        // To keep things simple division is only used if p2 is non zero; otherwise, use addition.
        return (p2 != 0 ? "/" : "+");
      default:
        return "+";
    }
  }

  /**
   * Checks the given answer for correctness.
   */
  private String checkAnswer() {
    boolean correct;
    switch (operation) {
      case "+":
        correct = (p1 + p2) == answer;
        break;
      case "-":
        correct = (p1 - p2) == answer;
        break;
      case "*":
        correct = (p1 * p2) == answer;
        break;
      case "/":
        // To keep things simple integer division is used.
        correct = (p1 / p2) == answer;
        break;
      default:
        correct = (p1 + p2) == answer;
        break;
    }
    return correct ? "correct" : "wrong";
  }

  String getOperation() {
    return operation;
  }

  int getP1() {
    return p1;
  }

  int getP2() {
    return p2;
  }

  void setAnswer(int answer) {
    this.answer = answer;
  }
}
