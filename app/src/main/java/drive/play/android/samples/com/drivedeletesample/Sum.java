/**
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
 * Represent a simple math sum and its answer.
 */
public class Sum {

    private String operation;
    private int p1;
    private int p2;
    private int ans;
    private Random random;

    public Sum() {
        random = new Random();
        p1 = generateInteger();
        p2 = generateInteger();
        operation = pickOperation();
    }

    public String toString() {
        return p1 + " " + operation + " " + p2 + " = " + ans + " (" + correction() + ")";
    }

    private int generateInteger() {
        return random.nextInt(10);
    }

    private String pickOperation() {
        int val = random.nextInt(4);
        switch(val) {
            case 0:
                return "+";
            case 1:
                return "-";
            case 2:
                return "*";
            case 3:
                // To keep things simple division is only used if p2 is non zero.
                // Otherwise use addition.
                if (p2 != 0) {
                    return "/";
                } else {
                    return "+";
                }
            default:
                return "+";
        }
    }

    /**
     * Check answer for correctness.
     *
     * @return Indication of whether or not answer is correct.
     */
    private String correction() {
        boolean correct;
        switch(operation) {
            case "+":
                correct = (p1 + p2) == ans;
                break;
            case "-":
                correct = (p1 - p2) == ans;
                break;
            case "*":
                correct = (p1 * p2) == ans;
                break;
            case "/":
                // To keep things simple integer division is used.
                correct = (p1 / p2) == ans;
                break;
            default:
                correct = (p1 + p2) == ans;
                break;

        }
        return correct ? "correct" : "wrong";
    }

    public String getOperation() {
        return operation;
    }

    public int getP1() {
        return p1;
    }

    public int getP2() {
        return p2;
    }

    public void setAns(int ans) {
        this.ans = ans;
    }
}
