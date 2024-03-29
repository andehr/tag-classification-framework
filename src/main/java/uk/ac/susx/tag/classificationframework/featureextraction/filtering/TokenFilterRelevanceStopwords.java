package uk.ac.susx.tag.classificationframework.featureextraction.filtering;

/*
 * #%L
 * TokenFilterRelevanceStopwords.java - classificationframework - CASM Consulting - 2,013
 * %%
 * Copyright (C) 2013 - 2014 CASM Consulting
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.collect.Sets;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.exceptions.FeatureExtractionException;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Set;
import java.util.stream.Collectors;

import java.util.Map;
import java.util.HashMap;

/**
 * This is a TokenFilter, see the TokenFilter abstract class for details of function and purpose of
 * token filters.
 *
 * This filters tokens if they are present in a stopword list. The stopword list is the one used in
 * the old framework, which seems to work okay for the relevance task.
 *
 * User: Andrew D. Robertson
 * Date: 27/07/2013
 * Time: 13:26
 */
public class TokenFilterRelevanceStopwords extends TokenFilter {

    private static final long serialVersionUID = 0L;
    private String language;

    private static final Map<String, Set<String>> STOPWORDS_BY_LANG = new HashMap<String, Set<String>>() {{
        put("en", Sets.newHashSet("a", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "all", "allow", "allows", "almost",
                "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", "anyway", "anyways",
                "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "around", "as", "aside", "ask", "asking", "associated", "at", "available", "away", "awfully", "b", "be", "became",
                "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between", "beyond", "both",
                "brief", "but", "by", "c", "came", "can", "cannot", "cant", "cause", "causes", "certain", "certainly", "changes", "clearly", "co", "com", "come", "comes", "concerning", "consequently",
                "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "course", "currently", "d", "definitely", "described", "despite", "did", "different", "do",
                "does", "doing", "done", "down", "downwards", "during", "e", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough", "entirely", "especially", "et", "etc", "even", "ever",
                "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "f", "far", "few", "fifth", "first", "five", "followed", "following", "follows", "for",
                "former", "formerly", "forth", "four", "from", "further", "furthermore", "g", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone", "got", "gotten", "greetings",
                "h", "had", "happens", "hardly", "has", "have", "having", "he", "hello", "help", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "hi", "him",
                "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i", "ie", "if", "ignored", "immediate", "in", "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates",
                "inner", "insofar", "instead", "into", "inward", "is", "it", "its", "itself", "j", "just", "k", "keep", "keeps", "kept", "know", "knows", "known", "l", "last", "lately", "later", "latter",
                "latterly", "least", "less", "lest", "let", "like", "liked", "likely", "little", "look", "looking", "looks", "ltd", "m", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely",
                "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "n", "name", "namely", "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless",
                "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing", "novel", "now", "nowhere", "o", "obviously", "of", "off", "often", "oh", "ok", "okay",
                "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", "overall", "own", "p", "particular",
                "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "q", "que", "quite", "qv", "r", "rather", "rd", "re", "really", "reasonably",
                "regarding", "regardless", "regards", "relatively", "respectively", "right", "s", "said", "same", "saw", "say", "saying", "says", "second", "secondly", "see", "seeing", "seem", "seemed",
                "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "since", "six", "so", "some", "somebody", "somehow",
                "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub", "such", "sup", "sure", "t", "take", "taken",
                "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore",
                "therein", "theres", "thereupon", "these", "they", "think", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together",
                "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "twice", "two", "u", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon",
                "us", "use", "used", "useful", "uses", "using", "usually", "uucp", "v", "value", "various", "very", "via", "viz", "vs", "w", "want", "wants", "was", "way", "we", "welcome", "well", "went",
                "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who",
                "whoever", "whole", "whom", "whose", "why", "will", "willing", "wish", "with", "within", "without", "wonder", "would", "would", "x", "y", "yes", "yet", "you", "your", "yours", "yourself",
                "yourselves", "z", "zero"));

        put("zh", Sets.newHashSet("--", "?", "“", "”", "》", "－－", "able", "about", "above", "according", "accordingly", "across", "actually", "after", "afterwards", "again", "against", "ain't", "all",
                "allow", "allows", "almost", "alone", "along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything",
                "anyway", "anyways", "anywhere", "apart", "appear", "appreciate", "appropriate", "are", "aren't", "around", "as", "a's", "aside", "ask", "asking", "associated", "at", "available", "away",
                "awfully", "be", "became", "because", "become", "becomes", "becoming", "been", "before", "beforehand", "behind", "being", "believe", "below", "beside", "besides", "best", "better", "between",
                "beyond", "both", "brief", "but", "by", "came", "can", "cannot", "cant", "can't", "cause", "causes", "certain", "certainly", "changes", "clearly", "c'mon", "co", "com", "come", "comes",
                "concerning", "consequently", "consider", "considering", "contain", "containing", "contains", "corresponding", "could", "couldn't", "course", "c's", "currently", "definitely", "described",
                "despite", "did", "didn't", "different", "do", "does", "doesn't", "doing", "done", "don't", "down", "downwards", "during", "each", "edu", "eg", "eight", "either", "else", "elsewhere", "enough",
                "entirely", "especially", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "exactly", "example", "except", "far", "few", "fifth", "first", "five",
                "followed", "following", "follows", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "get", "gets", "getting", "given", "gives", "go", "goes", "going", "gone",
                "got", "gotten", "greetings", "had", "hadn't", "happens", "hardly", "has", "hasn't", "have", "haven't", "having", "he", "hello", "help", "hence", "her", "here", "hereafter", "hereby", "herein",
                "here's", "hereupon", "hers", "herself", "he's", "hi", "him", "himself", "his", "hither", "hopefully", "how", "howbeit", "however", "i'd", "ie", "if", "ignored", "i'll", "i'm", "immediate", "in",
                "inasmuch", "inc", "indeed", "indicate", "indicated", "indicates", "inner", "insofar", "instead", "into", "inward", "is", "isn't", "it", "it'd", "it'll", "its", "it's", "itself", "i've",
                "just", "keep", "keeps", "kept", "know", "known", "knows", "last", "lately", "later", "latter", "latterly", "least", "less", "lest", "let", "let's", "like", "liked", "likely", "little", "look",
                "looking", "looks", "ltd", "mainly", "many", "may", "maybe", "me", "mean", "meanwhile", "merely", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "name", "namely",
                "nd", "near", "nearly", "necessary", "need", "needs", "neither", "never", "nevertheless", "new", "next", "nine", "no", "nobody", "non", "none", "noone", "nor", "normally", "not", "nothing",
                "novel", "now", "nowhere", "obviously", "of", "off", "often", "oh", "ok", "okay", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours",
                "ourselves", "out", "outside", "over", "overall", "own", "particular", "particularly", "per", "perhaps", "placed", "please", "plus", "possible", "presumably", "probably", "provides", "que",
                "quite", "qv", "rather", "rd", "re", "really", "reasonably", "regarding", "regardless", "regards", "relatively", "respectively", "right", "said", "same", "saw", "say", "saying", "says", "second",
                "secondly", "see", "seeing", "seem", "seemed", "seeming", "seems", "seen", "self", "selves", "sensible", "sent", "serious", "seriously", "seven", "several", "shall", "she", "should", "shouldn't",
                "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", "soon", "sorry", "specified", "specify", "specifying", "still", "sub",
                "such", "sup", "sure", "take", "taken", "tell", "tends", "th", "than", "thank", "thanks", "thanx", "that", "thats", "that's", "the", "their", "theirs", "them", "themselves", "then", "thence",
                "there", "thereafter", "thereby", "therefore", "therein", "theres", "there's", "thereupon", "these", "they", "they'd", "they'll", "they're", "they've", "think", "third", "this", "thorough",
                "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "took", "toward", "towards", "tried", "tries", "truly", "try", "trying", "t's", "twice",
                "two", "un", "under", "unfortunately", "unless", "unlikely", "until", "unto", "up", "upon", "us", "use", "used", "useful", "uses", "using", "usually", "value", "various", "very", "via", "viz",
                "vs", "want", "wants", "was", "wasn't", "way", "we", "we'd", "welcome", "well", "we'll", "went", "were", "we're", "weren't", "we've", "what", "whatever", "what's", "when", "whence", "whenever",
                "where", "whereafter", "whereas", "whereby", "wherein", "where's", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "who's", "whose", "why",
                "will", "willing", "wish", "with", "within", "without", "wonder", "won't", "would", "wouldn't", "yes", "yet", "you", "you'd", "you'll", "your", "you're", "yours", "yourself", "yourselves",
                "you've", "zero", "zt", "ZT", "zz", "ZZ", "一", "一下", "一些", "一切", "一则", "一天", "一定", "一方面", "一旦", "一时", "一来", "一样", "一次", "一片", "一直", "一致", "一般", "一起", "一边", "一面",
                "万一", "上下", "上升", "上去", "上来", "上述", "上面", "下列", "下去", "下来", "下面", "不一", "不久", "不仅", "不会", "不但", "不光", "不单", "不变", "不只", "不可", "不同", "不够", "不如", "不得", "不怕",
                "不惟", "不成", "不拘", "不敢", "不断", "不是", "不比", "不然", "不特", "不独", "不管", "不能", "不要", "不论", "不足", "不过", "不问", "与", "与其", "与否", "与此同时", "专门", "且", "两者", "严格", "严重",
                "个", "个人", "个别", "中小", "中间", "丰富", "临", "为", "为主", "为了", "为什么", "为什麽", "为何", "为着", "主张", "主要", "举行", "乃", "乃至", "么", "之", "之一", "之前", "之后", "之後", "之所以",
                "之类", "乌乎", "乎", "乘", "也", "也好", "也是", "也罢", "了", "了解", "争取", "于", "于是", "于是乎", "云云", "互相", "产生", "人们", "人家", "什么", "什么样", "什麽", "今后", "今天", "今年", "今後",
                "仍然", "从", "从事", "从而", "他", "他人", "他们", "他的", "代替", "以", "以上", "以下", "以为", "以便", "以免", "以前", "以及", "以后", "以外", "以後", "以来", "以至", "以至于", "以致", "们", "任",
                "任何", "任凭", "任务", "企图", "伟大", "似乎", "似的", "但", "但是", "何", "何况", "何处", "何时", "作为", "你", "你们", "你的", "使得", "使用", "例如", "依", "依照", "依靠", "促进", "保持", "俺", "俺们",
                "倘", "倘使", "倘或", "倘然", "倘若", "假使", "假如", "假若", "做到", "像", "允许", "充分", "先后", "先後", "先生", "全部", "全面", "兮", "共同", "关于", "其", "其一", "其中", "其二", "其他", "其余", "其它",
                "其实", "其次", "具体", "具体地说", "具体说来", "具有", "再者", "再说", "冒", "冲", "决定", "况且", "准备", "几", "几乎", "几时", "凭", "凭借", "出去", "出来", "出现", "分别", "则", "别", "别的", "别说", "到",
                "前后", "前者", "前进", "前面", "加之", "加以", "加入", "加强", "十分", "即", "即令", "即使", "即便", "即或", "即若", "却不", "原来", "又", "及", "及其", "及时", "及至", "双方", "反之", "反应", "反映",
                "反过来", "反过来说", "取得", "受到", "变成", "另", "另一方面", "另外", "只是", "只有", "只要", "只限", "叫", "叫做", "召开", "叮咚", "可", "可以", "可是", "可能", "可见", "各", "各个", "各人", "各位", "各地",
                "各种", "各级", "各自", "合理", "同", "同一", "同时", "同样", "后来", "后面", "向", "向着", "吓", "吗", "否则", "吧", "吧哒", "吱", "呀", "呃", "呕", "呗", "呜", "呜呼", "呢", "周围", "呵", "呸", "呼哧",
                "咋", "和", "咚", "咦", "咱", "咱们", "咳", "哇", "哈", "哈哈", "哉", "哎", "哎呀", "哎哟", "哗", "哟", "哦", "哩", "哪", "哪个", "哪些", "哪儿", "哪天", "哪年", "哪怕", "哪样", "哪边", "哪里", "哼", "哼唷",
                "唉", "啊", "啐", "啥", "啦", "啪达", "喂", "喏", "喔唷", "嗡嗡", "嗬", "嗯", "嗳", "嘎", "嘎登", "嘘", "嘛", "嘻", "嘿", "因", "因为", "因此", "因而", "固然", "在", "在下", "地", "坚决", "坚持", "基本",
                "处理", "复杂", "多", "多少", "多数", "多次", "大力", "大多数", "大大", "大家", "大批", "大约", "大量", "失去", "她", "她们", "她的", "好的", "好象", "如", "如上所述", "如下", "如何", "如其", "如果", "如此",
                "如若", "存在", "宁", "宁可", "宁愿", "宁肯", "它", "它们", "它们的", "它的", "安全", "完全", "完成", "实现", "实际", "宣布", "容易", "密切", "对", "对于", "对应", "将", "少数", "尔后", "尚且", "尤其", "就",
                "就是", "就是说", "尽", "尽管", "属于", "岂但", "左右", "巨大", "巩固", "己", "已经", "帮助", "常常", "并", "并不", "并不是", "并且", "并没有", "广大", "广泛", "应当", "应用", "应该", "开外", "开始", "开展",
                "引起", "强烈", "强调", "归", "当", "当前", "当时", "当然", "当着", "形成", "彻底", "彼", "彼此", "往", "往往", "待", "後来", "後面", "得", "得出", "得到", "心里", "必然", "必要", "必须", "怎", "怎么",
                "怎么办", "怎么样", "怎样", "怎麽", "总之", "总是", "总的来看", "总的来说", "总的说来", "总结", "总而言之", "恰恰相反", "您", "意思", "愿意", "慢说", "成为", "我", "我们", "我的", "或", "或是", "或者", "战斗",
                "所", "所以", "所有", "所谓", "打", "扩大", "把", "抑或", "拿", "按", "按照", "换句话说", "换言之", "据", "掌握", "接着", "接著", "故", "故此", "整个", "方便", "方面", "旁人", "无宁", "无法", "无论", "既",
                "既是", "既然", "时候", "明显", "明确", "是", "是否", "是的", "显然", "显著", "普通", "普遍", "更加", "曾经", "替", "最后", "最大", "最好", "最後", "最近", "最高", "有", "有些", "有关", "有利", "有力",
                "有所", "有效", "有时", "有点", "有的", "有着", "有著", "望", "朝", "朝着", "本", "本着", "来", "来着", "极了", "构成", "果然", "果真", "某", "某个", "某些", "根据", "根本", "欢迎", "正在", "正如", "正常",
                "此", "此外", "此时", "此间", "毋宁", "每", "每个", "每天", "每年", "每当", "比", "比如", "比方", "比较", "毫不", "没有", "沿", "沿着", "注意", "深入", "清楚", "满足", "漫说", "焉", "然则", "然后", "然後",
                "然而", "照", "照着", "特别是", "特殊", "特点", "现代", "现在", "甚么", "甚而", "甚至", "用", "由", "由于", "由此可见", "的", "的话", "目前", "直到", "直接", "相似", "相信", "相反", "相同", "相对",
                "相对而言", "相应", "相当", "相等", "省得", "看出", "看到", "看来", "看看", "看见", "真是", "真正", "着", "着呢", "矣", "知道", "确定", "离", "积极", "移动", "突出", "突然", "立即", "第", "等", "等等",
                "管", "紧接着", "纵", "纵令", "纵使", "纵然", "练习", "组成", "经", "经常", "经过", "结合", "结果", "给", "绝对", "继续", "继而", "维持", "综上所述", "罢了", "考虑", "者", "而", "而且", "而况", "而外",
                "而已", "而是", "而言", "联系", "能", "能否", "能够", "腾", "自", "自个儿", "自从", "自各儿", "自家", "自己", "自身", "至", "至于", "良好", "若", "若是", "若非", "范围", "莫若", "获得", "虽", "虽则",
                "虽然", "虽说", "行为", "行动", "表明", "表示", "被", "要", "要不", "要不是", "要不然", "要么", "要是", "要求", "规定", "觉得", "认为", "认真", "认识", "让", "许多", "论", "设使", "设若", "该", "说明",
                "诸位", "谁", "谁知", "赶", "起", "起来", "起见", "趁", "趁着", "越是", "跟", "转动", "转变", "转贴", "较", "较之", "边", "达到", "迅速", "过", "过去", "过来", "运用", "还是", "还有", "这", "这个", "这么",
                "这么些", "这么样", "这么点儿", "这些", "这会儿", "这儿", "这就是说", "这时", "这样", "这点", "这种", "这边", "这里", "这麽", "进入", "进步", "进而", "进行", "连", "连同", "适应", "适当", "适用", "逐步", "逐渐",
                "通常", "通过", "造成", "遇到", "遭到", "避免", "那", "那个", "那么", "那么些", "那么样", "那些", "那会儿", "那儿", "那时", "那样", "那边", "那里", "那麽", "部分", "鄙人", "采取", "里面", "重大", "重新",
                "重要", "鉴于", "问题", "防止", "阿", "附近", "限制", "除", "除了", "除此之外", "除非", "随", "随着", "随著", "集中", "需要", "非但", "非常", "非徒", "靠", "顺", "顺着", "首先", "高兴", "是不是", "说说",
                " ", ""));

        /*
            Collection of stop-words from Arabic NLTK + normalised versions.
            Normalized versions added since the Stanford tokeniser normalizes the text and removes hamza
        */
        put("ar", Sets.newHashSet(
                "،", "ء", "ءَ", "آ", "آب", "آذار", "آض", "آل", "آمينَ", "آناء", "آنفا", "آه", "آهاً", "آهٍ", "آهِ", "أ", "أبدا", "أبريل", "أبو", "أبٌ", "أجل", "أجمع", "أحد", "أخبر", "أخذ", "أخو", "أخٌ", "أربع", "أربعاء",
                "أربعة", "أربعمئة", "أربعمائة", "أرى", "أسكن", "أصبح", "أصلا", "أضحى", "أطعم", "أعطى", "أعلم", "أغسطس", "أفريل", "أفعل به", "أفٍّ", "أقبل", "أكتوبر", "أل", "ألا", "ألف", "ألفى", "أم", "أما", "أمام",
                "أمامك", "أمامكَ", "أمد", "أمس", "أمسى", "أمّا", "أن", "أنا", "أنبأ", "أنت", "أنتم", "أنتما", "أنتن", "أنتِ", "أنشأ", "أنه", "أنًّ", "أنّى", "أهلا", "أو", "أوت", "أوشك", "أول", "أولئك", "أولاء", "أولالك",
                "أوّهْ", "أى", "أي", "أيا", "أيار", "أيضا", "أيلول", "أين", "أيّ", "أيّان", "أُفٍّ", "ؤ", "إحدى", "إذ", "إذا", "إذاً", "إذما", "إذن", "إزاء", "إلى", "إلي", "إليكم", "إليكما", "إليكنّ", "إليكَ", "إلَيْكَ", "إلّا",
                "إمّا", "إن", "إنَّ", "إى", "إياك", "إياكم", "إياكما", "إياكن", "إيانا", "إياه", "إياها", "إياهم", "إياهما", "إياهن", "إياي", "إيهٍ", "ئ", "ا", "ا?", "ا?ى", "االا", "االتى", "ابتدأ", "ابين", "اتخذ",
                "اثر", "اثنا", "اثنان", "اثني", "اثنين", "اجل", "احد", "اخرى", "اخلولق", "اذا", "اربعة", "اربعون", "اربعين", "ارتدّ", "استحال", "اصبح", "اضحى", "اطار", "اعادة", "اعلنت", "اف", "اكثر", "اكد", "الآن",
                "الألاء", "الألى", "الا", "الاخيرة", "الان", "الاول", "الاولى", "التى", "التي", "الثاني", "الثانية", "الحالي", "الذاتي", "الذى", "الذي", "الذين", "السابق", "الف", "اللاتي", "اللتان", "اللتيا", "اللتين",
                "اللذان", "اللذين", "اللواتي", "الماضي", "المقبل", "الوقت", "الى", "الي", "اليه", "اليها", "اليوم", "اما", "امام", "امس", "امسى", "ان", "انبرى", "انقلب", "انه", "انها", "او", "اول", "اي", "ايار",
                "ايام", "ايضا", "ب", "بؤسا", "بإن", "بئس", "باء", "بات", "باسم", "بان", "بخٍ", "بد", "بدلا", "برس", "بسبب", "بسّ", "بشكل", "بضع", "بطآن", "بعد", "بعدا", "بعض", "بغتة", "بل", "بلى", "بن", "به", "بها",
                "بهذا", "بيد", "بين", "بَسْ", "بَلْهَ", "ة", "ت", "تاء", "تارة", "تاسع", "تانِ", "تانِك", "تبدّل", "تجاه", "تحت", "تحوّل", "تخذ", "ترك", "تسع", "تسعة", "تسعمئة", "تسعمائة", "تسعون", "تسعين", "تشرين", "تعسا",
                "تعلَّم", "تفعلان", "تفعلون", "تفعلين", "تكون", "تلقاء", "تلك", "تم", "تموز", "تينك", "تَيْنِ", "تِه", "تِي", "ث", "ثاء", "ثالث", "ثامن", "ثان", "ثاني", "ثلاث", "ثلاثاء", "ثلاثة", "ثلاثمئة", "ثلاثمائة",
                "ثلاثون", "ثلاثين", "ثم", "ثمان", "ثمانمئة", "ثمانون", "ثماني", "ثمانية", "ثمانين", "ثمنمئة", "ثمَّ", "ثمّ", "ثمّة", "ج", "جانفي", "جدا", "جعل", "جلل", "جمعة", "جميع", "جنيه", "جوان", "جويلية", "جير",
                "جيم", "ح", "حاء", "حادي", "حار", "حاشا", "حاليا", "حاي", "حبذا", "حبيب", "حتى", "حجا", "حدَث", "حرى", "حزيران", "حسب", "حقا", "حمدا", "حمو", "حمٌ", "حوالى", "حول", "حيث", "حيثما", "حين", "حيَّ", "حَذارِ",
                "خ", "خاء", "خاصة", "خال", "خامس", "خبَّر", "خلا", "خلافا", "خلال", "خلف", "خمس", "خمسة", "خمسمئة", "خمسمائة", "خمسون", "خمسين", "خميس", "د", "دال", "درهم", "درى", "دواليك", "دولار", "دون", "دونك", "ديسمبر",
                "دينار", "ذ", "ذا", "ذات", "ذاك", "ذال", "ذانك", "ذانِ", "ذلك", "ذهب", "ذو", "ذيت", "ذينك", "ذَيْنِ", "ذِه", "ذِي", "ر", "رأى", "راء", "رابع", "راح", "رجع", "رزق", "رويدك", "ريال", "ريث", "رُبَّ", "ز", "زاي",
                "زعم", "زود", "زيارة", "س", "ساء", "سابع", "سادس", "سبت", "سبتمبر", "سبحان", "سبع", "سبعة", "سبعمئة", "سبعمائة", "سبعون", "سبعين", "ست", "ستة", "ستكون", "ستمئة", "ستمائة", "ستون", "ستين", "سحقا", "سرا",
                "سرعان", "سقى", "سمعا", "سنة", "سنتيم", "سنوات", "سوف", "سوى", "سين", "ش", "شباط", "شبه", "شتانَ", "شخصا", "شرع", "شمال", "شيكل", "شين", "شَتَّانَ", "ص", "صاد", "صار", "صباح", "صبر", "صبرا", "صدقا",
                "صراحة", "صفر", "صهٍ", "صهْ", "ض", "ضاد", "ضحوة", "ضد", "ضمن", "ط", "طاء", "طاق", "طالما", "طرا", "طفق", "طَق", "ظ", "ظاء", "ظل", "ظلّ", "ظنَّ", "ع", "عاد", "عاشر", "عام", "عاما", "عامة", "عجبا", "عدا",
                "عدة", "عدد", "عدم", "عدَّ", "عسى", "عشر", "عشرة", "عشرون", "عشرين", "عل", "علق", "علم", "على", "علي", "عليك", "عليه", "عليها", "علًّ", "عن", "عند", "عندما", "عنه", "عنها", "عوض", "عيانا", "عين", "عَدَسْ",
                "غ", "غادر", "غالبا", "غدا", "غداة", "غير", "غين", "ـ", "ف", "فإن", "فاء", "فان", "فانه", "فبراير", "فرادى", "فضلا", "فقد", "فقط", "فكان", "فلان", "فلس", "فهو", "فو", "فوق", "فى", "في", "فيفري", "فيه",
                "فيها", "ق", "قاطبة", "قاف", "قال", "قام", "قبل", "قد", "قرش", "قطّ", "قلما", "قوة", "ك", "كأن", "كأنّ", "كأيّ", "كأيّن", "كاد", "كاف", "كان", "كانت", "كانون", "كثيرا", "كذا", "كذلك", "كرب", "كسا", "كل",
                "كلتا", "كلم", "كلَّا", "كلّما", "كم", "كما", "كن", "كى", "كيت", "كيف", "كيفما", "كِخ", "ل", "لأن", "لا", "لا سيما", "لات", "لازال", "لاسيما", "لام", "لايزال", "لبيك", "لدن", "لدى", "لدي", "لذلك", "لعل", "لعلَّ",
                "لعمر", "لقاء", "لكن", "لكنه", "لكنَّ", "للامم", "لم", "لما", "لمّا", "لن", "له", "لها", "لهذا", "لهم", "لو", "لوكالة", "لولا", "لوما", "ليت", "ليرة", "ليس", "ليسب", "م", "مئة", "مئتان", "ما", "ما أفعله",
                "ما انفك", "ما برح", "مائة", "ماانفك", "مابرح", "مادام", "ماذا", "مارس", "مازال", "مافتئ", "ماي", "مايزال", "مايو", "متى", "مثل", "مذ", "مرّة", "مساء", "مع", "معاذ", "معه", "مقابل", "مكانكم", "مكانكما",
                "مكانكنّ", "مكانَك", "مليار", "مليم", "مليون", "مما", "من", "منذ", "منه", "منها", "مه", "مهما", "ميم", "ن", "نا", "نبَّا", "نحن", "نحو", "نعم", "نفس", "نفسه", "نهاية", "نوفمبر", "نون", "نيسان", "نيف", "نَخْ", "نَّ",
                "ه", "هؤلاء", "ها", "هاء", "هاكَ", "هبّ", "هذا", "هذه", "هل", "هللة", "هلم", "هلّا", "هم", "هما", "همزة", "هن", "هنا", "هناك", "هنالك", "هو", "هي", "هيا", "هيهات", "هيّا", "هَؤلاء", "هَاتانِ", "هَاتَيْنِ", "هَاتِه",
                "هَاتِي", "هَجْ", "هَذا", "هَذانِ", "هَذَيْنِ", "هَذِه", "هَذِي", "هَيْهات", "و", "و6", "وأبو", "وأن", "وا", "واحد", "واضاف", "واضافت", "واكد", "والتي", "والذي", "وان", "واهاً", "واو", "واوضح", "وبين", "وثي", "وجد", "وراءَك",
                "ورد", "وعلى", "وفي", "وقال", "وقالت", "وقد", "وقف", "وكان", "وكانت", "ولا", "ولايزال", "ولكن", "ولم", "وله", "وليس", "ومع", "ومن", "وهب", "وهذا", "وهو", "وهي", "وَيْ", "وُشْكَانَ", "ى", "ي", "ياء", "يفعلان",
                "يفعلون", "يكون", "يلي", "يمكن", "يمين", "ين", "يناير", "يوان", "يورو", "يوليو", "يوم", "يونيو", "ّأيّان"));

        put("de", Sets.newHashSet("a", "ab", "aber","ach","acht","achte","achten","achter","achtes","ag","alle","allein","allem","allen","aller","allerdings","alles","allgemeinen","als","also","am","an","ander","andere","anderem","anderen","anderer","anderes","anderm","andern","anderr","anders","au","auch","auf","aus","ausser","ausserdem","außer","außerdem","b","bald","bei","beide","beiden","beim","beispiel","bekannt","bereits",
                "besonders","besser","besten","bin","bis","bisher","bist","c","d","d.h","da","dabei","dadurch","dafür","dagegen","daher","dahin","dahinter","damals","damit","danach","daneben","dank","dann","daran","darauf","daraus","darf","darfst","darin","darum","darunter","darüber","das","dasein","daselbst","dass","dasselbe","davon","davor","dazu","dazwischen",
                "daß","dein","deine","deinem","deinen","deiner","deines","dem","dementsprechend","demgegenüber","demgemäss","demgemäß","demselben","demzufolge","den","denen","denn","denselben","der","deren","derer","derjenige","derjenigen","dermassen","dermaßen","derselbe","derselben","des","deshalb","desselben","dessen","deswegen","dich","die","diejenige","diejenigen","dies","diese","dieselbe","dieselben","diesem","diesen","dieser",
                "dieses","dir","doch","dort","drei","drin","dritte","dritten","dritter","drittes","du","durch","durchaus","durfte","durften","dürfen","dürft","e","eben","ebenso","ehrlich","ei","ei,","eigen","eigene","eigenen","eigener","eigenes","ein","einander","eine","einem","einen","einer","eines","einig","einige","einigem","einigen","einiger","einiges",
                "einmal","eins","elf","en","ende","endlich","entweder","er","ernst","erst","erste","ersten","erster","erstes","es","etwa","etwas","euch","euer","eure","eurem","euren","eurer","eures","f","folgende","früher","fünf","fünfte","fünften","fünfter","fünftes","für","g","gab","ganz","ganze","ganzen","ganzer","ganzes","gar","gedurft","gegen","gegenüber","gehabt","gehen","geht","gekannt","gekonnt","gemacht","gemocht","gemusst","genug","gerade","gern","gesagt","geschweige",
                "gewesen","gewollt","geworden","gibt","ging","gleich","gott","gross","grosse","grossen","grosser","grosses","groß","große","großen","großer","großes","gut","gute","guter","gutes","h","hab","habe","haben","habt","hast","hat","hatte","hatten","hattest","hattet","heisst","her","heute","hier","hin","hinter","hoch","hätte","hätten","i","ich","ihm","ihn","ihnen","ihr","ihre","ihrem","ihren","ihrer","ihres",
                "im","immer","in","indem","infolgedessen","ins","irgend","ist","j","ja","jahr","jahre","jahren","je","jede","jedem","jeden","jeder","jedermann","jedermanns","jedes","jedoch","jemand","jemandem","jemanden","jene","jenem","jenen","jener","jenes","jetzt","k","kam","kann","kannst","kaum",
                "kein","keine","keinem","keinen","keiner","keines","kleine","kleinen","kleiner","kleines","kommen","kommt","konnte","konnten","kurz","können","könnt","könnte","l","lang","lange","leicht","leide","lieber","los","m","machen","macht","machte","mag","magst","mahn","mal","man","manche","manchem","manchen","mancher","manches","mann","mehr","mein","meine","meinem",
                "meinen","meiner","meines","mensch","menschen","mich","mir","mit","mittel","mochte","mochten","morgen","muss","musst","musste","mussten","muß","mußt","möchte","mögen","möglich","mögt","müssen","müsst","müßt","n","na","nach","nachdem","nahm","natürlich","neben","nein","neue","neuen","neun","neunte","neunten","neunter","neuntes","nicht","nichts","nie","niemand","niemandem","niemanden","noch","nun","nur","o","ob","oben",
                "oder","offen","oft","ohne","ordnung","p","q","r","recht","rechte","rechten","rechter","rechtes","richtig","rund","s","sa","sache","sagt","sagte","sah","satt","schlecht","schluss","schon","sechs","sechste","sechsten","sechster","sechstes","sehr","sei","seid","seien","sein","seine","seinem","seinen","seiner","seines","seit","seitdem","selbst","sich","sie","sieben",
                "siebente","siebenten","siebenter","siebentes","sind","so","solang","solche","solchem","solchen","solcher","solches","soll","sollen","sollst","sollt","sollte","sollten","sondern","sonst","soweit","sowie","später","startseite","statt","steht","suche","t","tag","tage","tagen","tat","teil","tel","tritt","trotzdem","tun","u","uhr","um","und","uns","unse","unsem",
                "unsen","unser","unsere","unserer","unses","unter","v","vergangenen","viel","viele","vielem","vielen","vielleicht","vier","vierte","vierten","vierter","viertes","vom","von","vor","w","wahr","wann","war","waren","warst","wart","warum","was","weg","wegen","weil","weit","weiter","weitere","weiteren","weiteres","welche","welchem","welchen",
                "welcher","welches","wem","wen","wenig","wenige","weniger","weniges","wenigstens","wenn","wer","werde","werden","werdet","weshalb","wessen","wie","wieder","wieso","will","willst","wir","wird","wirklich","wirst","wissen","wo","woher","wohin","wohl","wollen","wollt","wollte","wollten","worden","wurde","wurden","während","währenddem","währenddessen","wäre","würde",
                "würden","x","y","z","z.b","zehn","zehnte","zehnten","zehnter","zehntes","zeit","zu","zuerst","zugleich","zum","zunächst","zur","zurück","zusammen","zwanzig","zwar","zwei","zweite","zweiten","zweiter","zweites","zwischen","zwölf","über","überhaupt","übrigens"));
    }};

    public static boolean supportedLanguages(String lang) {
        return STOPWORDS_BY_LANG.keySet().contains(lang);
    }

    public TokenFilterRelevanceStopwords(String lang){
        language = lang;

        if (!STOPWORDS_BY_LANG.containsKey(language)){
            throw new FeatureExtractionException("Language not supported: " + language);
        }
    }

    public boolean filter(int index, Document tokens){
        return getStopwords().contains(tokens.get(index).get("form").toLowerCase());
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
//        stopwords = getStopwords();
    }

    public static Set<Integer> getIndexedStopwords(FeatureExtractionPipeline pipeline, String lang) {
        return getStopwords(lang).stream()
                .map(pipeline::featureIndex)
                .collect(Collectors.toSet());
    }
    public Set<Integer> getIndexedStopwords(FeatureExtractionPipeline pipeline) {
        return getStopwords().stream()
            .map(pipeline::featureIndex)
            .collect(Collectors.toSet());
    }

    public String getLanguage() {
        // `null` check for back-compat
        return language == null ? "en" : language;
    }

    public Set<String> getStopwords() {
        return getStopwords(getLanguage());
    }

    public static Set<String> getStopwords(String lang) {
        return STOPWORDS_BY_LANG.get(lang);
    }

    @Override
    public boolean isThreadSafe() {
        return true;
    }
}
