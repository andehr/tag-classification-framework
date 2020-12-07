package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

import com.google.common.collect.ImmutableMap;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import uk.ac.susx.tag.classificationframework.datastructures.Document;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.FeatureExtractionPipeline;
import uk.ac.susx.tag.classificationframework.featureextraction.pipelines.PipelineBuilder;

/**
 * Created by ci53 on 07/12/2020.
 */
public class TokeniserChineseStanfordTest {

    private static FeatureExtractionPipeline pipeline;

    @BeforeClass
    public static void setupPipeline() {
         pipeline = new PipelineBuilder().build(new PipelineBuilder.OptionList()
                .add("tokeniser", ImmutableMap.of(
                        "type", "chinesestanford",
                        "filter_punctuation", true,
                        "normalise_urls", true,
                        "lower_case", true
                        )
                )
                .add("remove_stopwords", ImmutableMap.of(
                        "use", "true",
                        "lang", "zh"))
                .add("filter_regex", "[\\-（()）【\\[\\]】]")
                .add("unigrams", true)
        );
    }

    @AfterClass
    public static void teardownPipeline() {
        pipeline = null;
    }

    public FeatureExtractionPipeline getPipeline() {
        return pipeline;
    }

    private Instance buildInstance(String text) {
        return new Instance("", text, "");
    }

    @Test
    public void tokenise1() throws Exception {
        FeatureExtractionPipeline pipeline = getPipeline();

        String testString = "没有什么可比性\uD83D\uDE3A, 哈哈我知道了, \uE310，每一个认真。\uD869\uDFDD你知道吗 \uD850\uDE26 ，喜来登酒店";
        pipeline.extractUnindexedFeatures(new Instance("", testString, "")).forEach(System.out::println);
    }

    @Test
    public void testTokenise2() {
        String testString = "知识就是力量。Knowledge is power. 我";
        pipeline.extractUnindexedFeatures(new Instance("", testString, "")).forEach(System.out::println);
    }

    @Test
    public void testTokenise3() {
        String testString = "张士贵，本名忽峍，字武安。虢州卢氏人(今河南省卢氏县)，唐代名将。臂力过人，善骑射。《册府元龟》载他「膂力过人，弯弓一百五十斤，左右骑射，矢不虚发。」\n" +
                "曾祖张俊任北魏银青光禄大夫、横野将军。祖父张和任北齐开府车骑将军。父张国仕隋朝历任陕县主簿，硖州录事参军，历阳县令，后以军功授大都督，定居虢州卢氏县。\n" +
                "隋朝大业末年，张士贵是农民起义军首领之一，自称大总管、怀义公，人称「忽峍贼」。李渊招降张士贵，授右光禄大夫，受相国府司马刘文静节度，大败伪熊州(今河南宜阳县)刺史郑仲达。义宁二年（618年），作为唐王世子左元帅李建成的副手，为第一军总管先锋，向东征讨。后召回京城，拜为通州刺史(今四川达县)。\n" +
                "武德元年(618年)五月，薛举入侵泾州，秦王李世民为西讨元帅，张士贵以先登之功，赏赐奴婢八十人，绢彩千余段，金一百三十挺，授上柱国。后负责运粮草至渑池时，大破王世充属下将领郭士衡的偷袭。\n" +
                "武德二年(619年)，奉命剿灭土匪苏径，进击陆浑，授马军总管，经略熊州(今河南宜阳县)，抵御王世充。赐爵为新野县开国公。\n" +
                "刘武周与突厥联军攻破榆次、介州，进围太原。齐王李元吉弃城而逃，关中震动。唐高祖诏秦王李世民督兵进讨，驻军柏壁。命张士贵攻打虞州(今山西运城东北安邑镇)的何小董。武德三年(620年)4月，在雀鼠谷之战和洺州之战中，唐军大破宋金刚、刘武周。\n" +
                "7月，李世民率军继续征讨洛阳的王世充，张士贵负责督运粮草。以功特派遣殷开山、杜如晦带金银四百余挺赏赐张士贵等诸将。平定洛阳后，累计战功，授虢州刺史。随后继续参与讨伐刘黑闼、徐圆朗，成为李世民秦王府的右库真、骠骑将军。\n" +
                "武德九年六月初四（626年）玄武门之变时张士贵也参与其中，李世民成为太子后，任太子内率。与刘师立募兵万余，拜为右骁卫将军。之后镇守玄武门，不久转为右屯卫将军。\n" +
                "贞观六年（632年）8月，除右武候将军，贞观七年（633年），拜龚州道行军总管，征讨桂州东西王洞獠民叛乱，破反獠而还，唐太宗听闻其冒矢石先登，慰劳张士贵道：“尝闻以忠报国者不顾身，于公见之。”授右屯卫大将军，改封虢国公，检校桂州都督。贞观十五年（641年）随唐太宗去洛阳宫。薛延陀入侵边境，张士贵奉命镇守庆州(甘肃庆阳市)，后任检校夏州都督。\n" +
                "贞观十八年（644年），唐太宗诏令调集粮草，招募军士，准备东征高句丽，龙门人薛仁贵投入张士贵麾下。十九年（645年）三月，张士贵跟随唐太宗征讨高句丽，隶属李世\uD869\uDFDD麾下为行军总管、洺州刺史，十月还师，以功拜冠军大将军、行左屯卫将军，并担任殿后，至并州时再次升为右屯卫大将军，授茂州都督。\n" +
                "唐太宗征讨高句丽时，下令剑南诸獠造船运兵粮，雅、邛、眉三州山獠因不堪其扰，相率叛乱，唐太宗下诏发陇右、峡兵二万，以茂州都督张士贵为雅州道行军总管，与右卫将军梁建方平之。事平，拜金紫光禄大夫、扬州都督府长史。\n" ;
        pipeline.extractUnindexedFeatures(new Instance("", testString, "")).forEach(System.out::println);
    }

    @Test
    public void testTokenise4() {
        String testString = "我们的收费非常不合理，在这里住的四五天令人非常不开心";
        pipeline.extractUnindexedFeatures(new Instance("", testString, "")).forEach(System.out::println);
    }

    @Test
    public void testTokenise5() {
        String testString = "没有什么可比性\uD83D\uDE3A";
        pipeline.extractUnindexedFeatures(new Instance("", testString, "")).forEach(System.out::println);
    }

    @Test
    public void testTokenise6() {
        String testString = "四足形類 维基百科，自由的百科全书 跳到导航 跳到搜索 四足形上綱 化石时期：409–0  Ma PreЄ Є O S D C P T J K Pg N 泥盆纪 早期 → 现代 提塔利克鱼 ，生存于泥盆纪晚期的高等 肉鳍鱼 ，属于 希望螈类 ，是 四足动物 的旁系远亲。 科学分类 界： 动物界 Animalia 门： 脊索动物门 Chordata 高纲： 硬骨鱼高纲 Osteichthyes 总纲： 肉鳍鱼总纲 Sarcopterygii 纲： 肺鱼四足纲 Dipnotetrapodomorpha 亚纲： 四足形亚纲 Tetrapodomorpha Ahlberg , 1991 下级分类 † 肯氏鱼属 （英语： Kenichthys ） Kenichthys † 根齿鱼目 （英语： Rhizodontida ） Rhizodontida † 卡南德拉鱼科 （英语： Canowindridae ） Canowindridae † 骨鳞鱼目 （英语： Osteolepiformes ） Osteolepiformes （或 † 巨鱼目 （英语： Megalichthyiformes ） Megalichthyiformes） 始四足类 （英语： Eotetrapodiformes ） Eotetrapodiformes 四足形上綱（ 學名 ：Tetrapodomorpha，或 Choanata [1] ）通称 四足形类，是 肉鳍鱼总纲 的一个 演化支 ，包含 四足類 和一群介於魚類与四足類之間的史前過渡物種，這些史前物種顯示了海生肉鳍鱼发展为陆生四足类的演化历程。 肺魚 是四足形类现存最亲近的 旁系群 ，二者被一同归为 扇鳍类 （肺鱼四足形大纲）。 四足形类的 化石 在 4 亿年前的 泥盆纪 早期便已开始出现，包括 骨鳞鱼 （英语： Osteolepis ）（Osteolepis）、 潘氏鱼 （Panderichthys）、 肯氏鱼 （英语： Kenichthys ）（Kenichthys）和 东生鱼 （Tungsenia）等 [2] 。尽管肉鳍鱼总纲的鱼形动物至今已凋零殆尽， 生态位 被 辐鳍鱼 所取代，但四足形动物自 中生代 以来便成为地球上的优势动物，以极其丰富的种类占据多种生境，是肉鳍鱼现存的主要后代。 四足形类的一大特征是四肢的进化，即胸鳍和腹鳍演变为前足和后足。另一关键特征是鼻孔的移位——原始肉鳍鱼类的前后两对鼻孔长在头部两侧，分别用于进水和排水，而早期四足形动物（如肯氏鱼）的后鼻孔已下移至嘴边，较晚出现的四足形动物（如现代四足类）的后鼻孔则转移至口腔内部 [3] 。 系统发生[ 编辑 ] 根据 2017 年《 硬骨鱼支序分类法 》，四足形类和其他 現生 硬骨鱼 的演化关系如下： [4] [5] [6] 硬骨鱼高纲 Osteichthyes 辐鳍鱼总纲 Actinopterygii 辐鳍鱼纲 Actinopteri 新鳍亚纲 Neopterygii   真骨下纲 Teleostei     全骨下纲 Holostei       软质亚纲 Chondrostei       腕鳍鱼纲 Cladistia     肉鳍鱼总纲 Sarcopterygii 肺鱼四足纲 Dipnotetrapodomorpha   肺鱼亚纲 Dipnomorpha     四足形亚纲 Tetrapodomorpha       腔棘魚綱 Coelacanthimorpha         軟骨魚綱 Chondrichthyes （ 外类群 ）   下级分类[ 编辑 ] 泥盆紀 晚期相继出现的 肉鳍鱼 后代。 潘氏魚 Panderichthys：适合在淤泥浅滩中生活。 提塔利克魚 Tiktaalik：鱼鳍类似四足动物的脚，能使其走上陸地。 魚石螈 Ichthyostega：四足具备。 棘螈 Acanthostega：四足各有八趾。 四足形类是 肉鳍鱼 的主要 演化支 ，由现代 四足类动物 及其史前亲族构成。四足类的史前亲族包含原始的水生肉鳍鱼，和由它们演化而成的形似 蝾螈 的古代 两栖动物 ，以及处在此二者之间的各类过渡物种，这些肉鳍鱼类和现存四足动物的关系比和 肺魚 更为亲近（Amemiya 等人，2013 年）。 在 系统发生学 上，四足形类是四足动物的 总群 ，而现存四足动物（及其 最近共同祖先 的已灭绝后代）是四足形类的 冠群 ，除去这个冠群后则剩下 并系 的四足动物 幹群 ，囊括了从肉鳍鱼演化至四足动物的一系列史前过渡类群，其中 卡南德拉鱼科 （英语： Canowindridae ）、 骨鳞鱼目 （英语： Osteolepiformes ）（或 巨鱼目 （英语： Megalichthyiformes ））及 三列鳍鱼科 （英语： Tristichopteridae ）被统一归为 骨鳞鱼总目 （英语： Osteolepidida ）（Osteolepidida），但由于三列鳍鱼科为 始四足类 （英语： Eotetrapodiformes ）的演化支，而始四足类的其他分支未被骨鳞鱼总目涵盖，因此骨鳞鱼总目是不合理的并系群。 根据 2012 年 伯克利加州大学 学者 Swartz 对 46 个相关类群的 204 个特征进行的 系统发育 分析，四足形类的内部分化关系如下： [7] 四足形类    † 肯氏鱼属 Kenichthys       † 根齿鱼目 Rhizodontida       † 卡南德拉鱼科 Canowindridae   † Marsdenichthys       † 卡南德拉鱼属 Canowindra       † Koharalepis     † Beelarongia             † 骨鳞鱼目 Osteolepiformes   † 格格纳瑟斯鱼属 Gogonasus       † Gyroptychius       † 骨鳞鱼科 Osteolepidae       † Medoevia     † 巨鱼科 Megalichthyidae           始四足类 Eotetrapodiformes † 三列鳍鱼科 Tristichopteridae   † Spodichthys       † 三列鳍鱼属 Tristichopterus       † 真掌鳍鱼属 Eusthenopteron       † Jarvikina       † 石炭鱼属 Cabonnichthys       † Mandageria     † 真掌齿鱼属 Eusthenodon                   † 提尼拉鱼属 Tinirau       † 扁头鱼属 Platycephalichthys   希望螈类 Elpistostegalia   † 潘氏鱼属 Panderichthys   坚头类 Stegocephalia     † 提塔利克鱼属 Tiktaalik     † 希望螈属 Elpistostege         † 散步鱼属 Elginerpeton       † 孔螈属 Ventastega       † 棘螈属 Acanthostega       † 鱼石螈属 Ichthyostega       † 瓦切螈科 Whatcheeriidae       † 圆螈科 Colosteidae       † 厚蛙螈属 Crassigyrinus       † 斜眼螈总科 Baphetoidea     四足類 Tetrapoda（ 冠群 ）                                     参考文献[ 编辑 ] 维基共享资源 中相关的多媒体资源： 四足形類 维基物种 中的分类信息： 四足形類 ^ Zhu Min; Schultze, Hans-Peter.  Per Erik Ahlberg, 编. Major Events in Early Vertebrate Evolution . CRC Press. 11 September 2002: 296 [5 August 2015]. ISBN 978-0-203-46803-6 .   ^ Jing Lu, Min Zhu, John A. Long, Wenjin Zhao, Tim J. Senden, Liantao Jia and Tuo Qiao. The earliest known stem-tetrapod from the Lower Devonian of China. Nature Communications. 2012, 3: 1160. Bibcode:2012NatCo...3.1160L . PMID 23093197 . doi:10.1038/ncomms2170 .   ^ Clack, Jennifer A. Gaining Ground: The Origin and Evolution of Tetrapods . Indiana University Press. 2012: 74 [8 June 2015]. ISBN 978-0-253-35675-8 . （原始内容 存档 于2019-12-16）.   ^ Betancur-R, Ricardo; Wiley, Edward O.; Arratia, Gloria; Acero, Arturo; Bailly, Nicolas; Miya, Masaki; Lecointre, Guillaume; Ortí, Guillermo. Phylogenetic classification of bony fishes . BMC Evolutionary Biology. 2017-07-06, 17: 162 [2019-01-13]. ISSN 1471-2148 . doi:10.1186/s12862-017-0958-3 . （ 原始内容 存档于2019-03-22）.   ^ Betancur-R, R., E. Wiley, N. Bailly, M. Miya, G. Lecointre, and G. Ortí. 2014. Phylogenetic Classification of Bony Fishes --Version 3 ( 存档副本 . [2015-08-09]. （ 原始内容 存档于2015-08-14）.  ). ^ Betancur-R., R., R.E. Broughton, E.O. Wiley, K. Carpenter, J.A. Lopez, C. Li, N.I. Holcroft, D. Arcila, M. Sanciangco, J. Cureton, F. Zhang, T. Buser, M. Campbell, T. Rowley, J.A. Ballesteros, G. Lu, T. Grande, G. Arratia & G. Ortí. 2013. The tree of life and a new classification of bony fishes. PLoS Currents Tree of Life . 2013 Apr 18. ^ Swartz, B. A marine stem-tetrapod from the Devonian of Western North America . PLoS ONE. 2012, 7 (3): e33683. PMC 3308997 . PMID 22448265 . doi:10.1371/journal.pone.0033683 . （原始内容 存档 于2014-12-17）.   Mikko Haaramo. Tetrapodomorpha – Terrestrial vertebrate-like sarcopterygians . [6 April 2006]. （ 原始内容 存档于12 May 2006）.   P. E. Ahlberg & Z. Johanson. Osteolepiforms and the ancestry of tetrapods. Nature . 1998, 395 (6704): 792–794. Bibcode:1998Natur.395..792A . doi:10.1038/27421 .   Michel Laurin, Marc Girondot & Armand de Ricqlès. Early tetrapod evolution (PDF). TREE. 2000, 15 (3) [2020-09-05]. （ 原始内容 (PDF)存档于2009-09-22）.   查 论 编 魚類演化 （英语： Evolution_of_fish ） † 表示滅絕 脊索動物 頭索動物亞門 † 皮卡蟲 † 華夏鰻屬 文昌魚目 嗅球類 † 海口蟲屬 （英语： Haikouella ） 被囊動物亞門 † 昆明魚目 （英语： Myllokunmingiidae ）? † 中新魚屬 （英语： Zhongxiniscus ）? 無頜總綱 圓口綱 盲鰻 七鰓鰻亞綱 † 海口魚 七鰓鰻目 † 牙形石 † 原牙形石目 （英语： Protoconodont ）? † 副牙形石目 （英语： Paraconodontida ） † 鋸齒刺目 （英语： Prioniodontida ） † 應許牙石屬 （英语： Promissum ） † 甲冑魚 † 鰭甲魚綱 † 花鱗魚綱 （英语： Thelodonti ） † 缺甲魚綱 † 頭甲魚類 † 盔甲魚綱 （英语： Galeaspida ） † 茄甲魚綱 （英语： Pituriaspida ） † 骨甲魚綱 有頷下門 † 盾皮魚綱 † 胴甲魚目 † 節甲魚目 † 布林達貝拉魚目 （英语： Brindabellaspida ） † 瓣甲魚目 † 葉鱗魚目 † 褶齒魚目 （英语： Ptyctodontida ） † 硬鮫目 （英语： Rhenanida ） † 棘胸魚目 （英语： Acanthothoraci ） † 假瓣甲魚目 （英语： Pseudopetalichthyida ）? † 史天秀魚目 （英语： Stensioellida ）? † 棘魚綱 † 柵棘魚目 （英语： Climatiiformes ） † 銼棘魚目 （英语： Ischnacanthiformes ） 軟骨魚綱 板鰓亞綱 全頭亞綱 硬骨魚 肉鰭魚總綱 † 爪齒魚目 腔棘魚綱 腔棘魚目 肺魚形類 † 孔鱗魚目 肺魚總目 四足形類 輻鰭魚總綱 腕鰭魚綱 軟骨硬鱗亞綱 新鰭亞綱 † 半椎魚目 全骨下綱 真骨類 魚類列表 史前魚類列表 （英语： Lists of prehistoric fish ） 棘魚綱列表 （英语： List of acanthodians ） 盾皮魚列表 （英语： List of placoderm genera ） 史前軟骨魚列表 （英语： List of prehistoric cartilaginous fish genera ） 史前硬骨魚列表 （英语： List of prehistoric bony fish genera ） 肉鰭魚列表 （英语： List of sarcopterygian genera ） 過渡化石列表 （英语： List of transitional fossils ） 相關條目 Prehistoric life （英语： Prehistoric life ） 過渡化石 Vertebrate paleontology （英语： Vertebrate paleontology ） 查 论 编 現存的 脊索動物 類群 動物界 真後生動物亞界 两侧对称动物 後口動物總門 脊索動物門 頭索動物亞門 頭索綱 被囊動物亞門 海鞘纲 、 樽海鞘纲 、 尾海鞘綱 、 深水海鞘纲 （英语： Sorberacea ） 脊椎動物亞門 無頜總綱 圓口綱 有頜下門 軟骨魚綱 板鰓亞綱 、 全頭亞綱 硬骨魚高綱 輻鰭魚總綱 輻鰭魚綱 、 腕鰭魚綱 肉鰭魚總綱 腔棘魚綱 、 肺魚形類 四足形類 兩棲綱 → 离片椎目 → 滑體亞綱 羊膜類 合弓綱 真盤龍類 → 楔齒龍類 → 獸孔目 → 犬齒獸亞目 → 哺乳綱 蜥形綱 副爬行動物 、 真爬行動物 雙孔亞綱 鱗龍形下綱 喙頭目 、 有鳞目 （ 蜥蜴亚目 → 蛇亚目 ） 主龍形下綱 鱷目 、 龜鱉目 、 恐龍總目 → 鳥綱 物種識別信息 維基數據 : Q1209254 維基物種 : Tetrapodomorpha Fossilworks: 266402 取自“ https://zh.wikipedia.org/w/index.php?title=四足形類&oldid=62162249 ” 分类 ： 四足形類 1991年描述的分類群 隐藏分类： 物种微格式条目 含有拉丁語的條目 导航菜单 个人工具 没有登录 讨论 贡献 创建账户 登录 名字空间 条目 讨论 不转换 不转换 简体 繁體 大陆简体 香港繁體 澳門繁體 大马简体 新加坡简体 臺灣正體 视图 阅读 编辑 查看历史 更多 搜索 导航 首页 分类索引 特色内容 新闻动态 最近更改 随机条目 资助维基百科 帮助 帮助 维基社群 方针与指引 互助客栈 知识问答 字词转换 IRC即时聊天 联络我们 关于维基百科 工具 链入页面 相关更改 上传文件 特殊页面 固定链接 页面信息 引用本页 维基数据项 打印/导出 下载为PDF 打印页面 在其他项目中 维基共享资源 维基物种 其他语言 العربية Català English Español فارسی Suomi Français Bahasa Indonesia Italiano 한국어 Македонски Nederlands Polski Português Русский Simple English Українська Tiếng Việt 编辑链接 本页面最后修订于2020年10月4日 (星期日) 07:35。 本站的全部文字在 知识共享 署名-相同方式共享 3.0协议 之条款下提供，附加条款亦可能应用。（请参阅 使用条款 ） Wikipedia®和维基百科标志是 维基媒体基金会 的注册商标；维基™是维基媒体基金会的商标。 维基媒体基金会是按美国国內稅收法501(c)(3)登记的 非营利慈善机构 。 隐私政策 关于维基百科 免责声明 手机版视图 开发者 统计 Cookie声明 ";
        pipeline.extractUnindexedFeatures(new Instance("", testString, "")).forEach(System.out::println);
    }

    @Test
    public void testUppercasePreserved() {
        String testString = "ABC DEF GHI";
        FeatureExtractionPipeline pipeline = getPipeline();

        Instance testInstance = buildInstance(testString);
        Document obtainedDoc = pipeline.getTokeniser().tokenise(testInstance);

        System.out.println(obtainedDoc);
    }

    @Test
    public void testLowercasePreserved() {
        String testString = "abc def ghi";
        FeatureExtractionPipeline pipeline = getPipeline();

        Instance testInstance = buildInstance(testString);
        Document obtainedDoc = pipeline.getTokeniser().tokenise(testInstance);

        System.out.println(obtainedDoc);
    }

    @Test
    public void testEmojiPreserved() {
        String testString = "some emojis: \uD83C\uDDF3\uD83C\uDDFF\uD83E\uDD70 \uD83E\uDD70 \uD83E\uDD70";
        FeatureExtractionPipeline pipeline = getPipeline();

        Instance testInstance = buildInstance(testString);
        Document obtainedDoc = pipeline.getTokeniser().tokenise(testInstance);

        System.out.println(obtainedDoc);
    }

    @Test
    public void testEmptyString() {
        String testString = "";
        FeatureExtractionPipeline pipeline = getPipeline();

        Instance testInstance = buildInstance(testString);
        Document obtainedDoc = pipeline.getTokeniser().tokenise(testInstance);

        System.out.println(obtainedDoc);
    }

    @Test
    public void testOnlyEmoji() {
        String testString = "\uD83D\uDE03";
        FeatureExtractionPipeline pipeline = getPipeline();

        Instance testInstance = buildInstance(testString);
        Document obtainedDoc = pipeline.getTokeniser().tokenise(testInstance);

        System.out.println(obtainedDoc);
    }
}
