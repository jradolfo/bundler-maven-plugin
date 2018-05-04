package com.github.kospiotr.bundler.optimizer.support;

/**
 * Copyright 2015 wro4j
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

/**
 * JsMin.java.
 * <p>
 * Copyright (c) 2006 John Reilly (www.inconspicuous.org) This work is a
 * translation from C to Java of jsmin.c published by Douglas Crockford.
 * Permission is hereby granted to use the Java version under the same
 * conditions as the jsmin.c on which it is based.
 * <p>
 * http://www.crockford.com/javascript/jsmin.html
 *
 * @author Alex Objelean
 * @created Created on Dec 5, 2008
 */
@SuppressWarnings("serial")
public class JSMin {
    private static final int EOF = -1;

    private final PushbackInputStream in;

    private final OutputStream out;

    private int theA;

    private int theB;

    // Currently read character
    private int theX = EOF;
    // Previously read character
    private int theY = EOF;

    public JSMin(final InputStream in, final OutputStream out) {
        this.in = new PushbackInputStream(in);
        this.out = out;
    }

    /**
     * isAlphanum -- return true if the character is a letter, digit, underscore,
     * dollar sign, or non-ASCII character.
     */
    static boolean isAlphanum(final int c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                || (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || c == '\\' || c > 126);
    }

    /**
     * get -- return the next character from stdin. Watch out for lookahead. If
     * the character is a control character, translate it to a space or linefeed.
     */
    int get() throws IOException {
        final int c = in.read();

        if (c >= ' ' || c == '\n' || c == EOF) {
            return c;
        }

        if (c == '\r') {
            return '\n';
        }

        return ' ';
    }

    /**
     * Get the next character without getting it.
     */
    int peek() throws IOException {
        final int lookaheadChar = in.read();
        in.unread(lookaheadChar);
        return lookaheadChar;
    }

    /**
     * next -- get the next character, excluding comments. peek() is used to see
     * if a '/' is followed by a '/' or '*'.
     */
    int next() throws IOException, UnterminatedCommentException {
        int c = get();
        if (c == '/') {
            switch (peek()) {
                case '/':
                    for (; ; ) {
                        c = get();
                        if (c <= '\n') {
                            break;
                        }
                    }
                    break;
                case '*':
                    get();
                    while (c != ' ') {
                        switch (get()) {
                            case '*':
                                if (peek() == '/') {
                                    get();
                                    c = ' ';
                                }
                                break;
                            case EOF:
                                throw new UnterminatedCommentException();
                        }
                    }
                    break;
            }

        }
        theY = theX;
        theX = c;
        return c;
    }

    /**
     * action -- do something! What you do is determined by the argument:
     * <ul>
     * <li>1 Output A. Copy B to A. Get the next B.</li>
     * <li>2 Copy B to A. Get the next B. (Delete A).</li>
     * <li>3 Get the next B. (Delete B).</li>
     * </ul>
     * action treats a string as a single character. Wow!<br/>
     * action recognizes a regular expression if it is preceded by ( or , or =.
     */

    void action(final int d) throws IOException,
            UnterminatedRegExpLiteralException, UnterminatedCommentException,
            UnterminatedStringLiteralException {
        switch (d) {
            case 1:
                out.write(theA);
                if (theA == theB && (theA == '+' || theA == '-') && theY != theA) {
                    out.write(' ');
                }
            case 2:
                theA = theB;

                if (theA == '\'' || theA == '"' || theA == '`') {
                    for (; ; ) {
                        out.write(theA);
                        theA = get();
                        if (theA == theB) {
                            break;
                        }
                        if (theA <= '\n') {
                            throw new UnterminatedStringLiteralException();
                        }
                        if (theA == '\\') {
                            out.write(theA);
                            theA = get();
                        }
                    }
                }

            case 3:
                theB = next();
                if (theB == '/'
                        && (theA == '(' || theA == ',' || theA == '=' || theA == ':'
                        || theA == '[' || theA == '!' || theA == '&' || theA == '|'
                        || theA == '?' || theA == '+' || theA == '-' || theA == '~'
                        || theA == '*' || theA == '/' || theA == '{' || theA == '\n'
                        || theA == ' ')) {
                    // If a '/' comes after a space,
                    // - it might be a regular expression, e.g. return /^\d{3}$/.exec(val);
                    // - it might not be a regular expression. e.g. return a / b;
                    // But if we don't treat it as a regular expression, it will cause a problem.
                    // Just take the example above. /.exec(val); will be treated as a regular expression without a proper ending.
                    // TODO: This may not be a good fix, we will update it when we receive any report from users.
                    boolean isAfterSpace = theA == ' ';
                    out.write(theA);
                    if (theA == '/' || theA == '*') {
                        out.write(' ');
                    }
                    out.write(theB);
                    for (; ; ) {
                        theA = get();
                        if (theA == '[') {
                            for (; ; ) {
                                out.write(theA);
                                theA = get();
                                if (theA == ']') {
                                    break;
                                }
                                if (theA == '\\') {
                                    out.write(theA);
                                    theA = get();
                                }
                                if (theA <= '\n') {
                                    throw new UnterminatedRegExpLiteralException();
                                }
                            }
                        } else if (theA == '/') {
                            switch (peek()) {
                                case '/':
                                case '*':
                                    throw new UnterminatedRegExpLiteralException();
                            }
                            break;
                        } else if (theA == '\\') {
                            out.write(theA);
                            theA = get();
                        } else if (theA <= '\n') {
                            if (isAfterSpace) {
                                // This may not be a regular expression. Just break;
                            }
                            break;
                        }
                        out.write(theA);
                    }
                    theB = next();
                }
        }
    }

    /**
     * jsmin -- Copy the input to the output, deleting the characters which are
     * insignificant to JavaScript. Comments will be removed. Tabs will be
     * replaced with spaces. Carriage returns will be replaced with linefeeds.
     * Most spaces and linefeeds will be removed.
     */
    public void jsmin() throws IOException, UnterminatedRegExpLiteralException,
            UnterminatedCommentException, UnterminatedStringLiteralException {
        if (peek() == 0xEF) {
            get();
            get();
            get();
        }
        theA = '\n';
        // Get next B.
        action(3);
        while (theA != EOF) {
            switch (theA) {
                case ' ':
                    if (isAlphanum(theB)) {
                        action(1);
                    } else {
                        action(2);
                    }
                    break;
                case '\n':
                    switch (theB) {
                        case '{':
                        case '[':
                        case '(':
                        case '+':
                        case '-':
                        case '!':
                        case '~':
                            action(1);
                            break;
                        case ' ':
                            // Leading space, drop it.
                            action(3);
                            break;
                        default:
                            if (isAlphanum(theB)) {
                                action(1);
                            } else {
                                // We have a separator here. A new line is not needed. Drop it.
                                action(2);
                            }
                    }
                    break;
                default:
                    switch (theB) {
                        case ' ':
                            if (isAlphanum(theA)) {
                                action(1);
                                break;
                            }
                            // Space after separator, drop it.
                            action(3);
                            break;
                        case '\n':
                            switch (theA) {
                                case '}':
                                case ']':
                                case ')':
                                case '+':
                                case '-':
                                case '"':
                                case '\'':
                                case '`':
                                    action(1);
                                    break;
                                default:
                                    if (isAlphanum(theA)) {
                                        action(1);
                                    } else {
                                        action(3);
                                    }
                            }
                            break;
                        default:
                            action(1);
                            break;
                    }
            }
        }
        out.flush();
    }

    public static class UnterminatedCommentException extends Exception {
    }

    public static class UnterminatedStringLiteralException extends Exception {
    }

    public static class UnterminatedRegExpLiteralException extends Exception {
    }
}
