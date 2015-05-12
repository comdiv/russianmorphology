/**
 * Copyright 2009 Alexander Kuznetsov 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.lucene.morphology.analyzer;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.Morphology;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;


public class MorphologyFilter extends TokenFilter {
    private LuceneMorphology luceneMorph;
    private Iterator<String> iterator;
    private State state;
    public static int DEFAULT_PRESERVE_MORPHOLOGY_FLAG = 1<<27;
    public int preserveMorphologyFlag = DEFAULT_PRESERVE_MORPHOLOGY_FLAG;
    public boolean usePreserveFlag = false;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    private final FlagsAttribute flagAtt = addAttribute(FlagsAttribute.class);
    public MorphologyFilter(TokenStream tokenStream, LuceneMorphology luceneMorph,boolean usePreserveFlag){
        this(tokenStream,luceneMorph,usePreserveFlag,DEFAULT_PRESERVE_MORPHOLOGY_FLAG);
    }
    public MorphologyFilter(TokenStream tokenStream, LuceneMorphology luceneMorph,boolean usePreserveFlag, int preserveMorphologyFlag){
        this(tokenStream,luceneMorph);
        this.usePreserveFlag = usePreserveFlag;
        this.preserveMorphologyFlag = preserveMorphologyFlag;
    }
    public MorphologyFilter(TokenStream tokenStream, LuceneMorphology luceneMorph) {
        super(tokenStream);
        this.luceneMorph = luceneMorph;
    }

    final public boolean incrementToken() throws IOException {
        if (iterator != null) {
            if (iterator.hasNext()) {
                restoreState(state);
                posIncrAtt.setPositionIncrement(0);
                termAtt.setEmpty().append(iterator.next());
                return true;
            } else {
                state = null;
                iterator = null;
            }
        }
        while (true) {
            boolean b = input.incrementToken();
            if (!b) {
                return false;
            }
            if(usePreserveFlag) {
                int flags = flagAtt.getFlags();
                if (0 != (flags & preserveMorphologyFlag)) {
                    return true;
                }
            }

            if (termAtt.length() > 0) {
                String s = new String(termAtt.buffer(), 0, termAtt.length());
                boolean restoreFirstCharCase  =false;
                if(!s.toLowerCase().equals(s) && Character.isUpperCase(s.charAt(0)) ){
                    restoreFirstCharCase  = true;
                    s = s.toLowerCase();
                }
                if (luceneMorph.checkString(s)) {
                    List<String> forms = luceneMorph.getNormalForms(s);
                    if(restoreFirstCharCase && !forms.isEmpty()){
                        int len = forms.size();
                        for(int i=0;i<len;i++){
                            String lowercased = forms.get(i);
                            String restored = Character.toUpperCase( lowercased.charAt(0) ) + lowercased.substring(1);
                            forms.add(restored);
                        }
                    }
                    if (forms.isEmpty()) {
                        continue;
                    } else if (forms.size() == 1) {
                        termAtt.setEmpty().append(forms.get(0));
                    } else {
                        state = captureState();
                        iterator = forms.iterator();
                        termAtt.setEmpty().append(iterator.next());
                    }
                }
            }
            return true;
        }
    }

}
