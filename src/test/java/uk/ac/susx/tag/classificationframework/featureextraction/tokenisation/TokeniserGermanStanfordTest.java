package uk.ac.susx.tag.classificationframework.featureextraction.tokenisation;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.susx.tag.classificationframework.datastructures.Instance;

/**
 * Created by ci53 on 07/12/2020.
 */
public class TokeniserGermanStanfordTest {
    @Test
    public void tokenise() throws Exception {
        Instance i = new Instance("", "Weitere Schreibweise sind Wojwode/Vojvode, Wojwoda/Vojvoda, Woiwod, Воевода/Wojewoda und Войвода/Wojwoda, hergeleitet aus dem Slawischen Войвода/Wojwoda. Die Bezeichnung Woiwodschaft ist von Woiwode abgeleitet. Die Region Banat, die heute in Serbien, Ungarn und Rumänien liegt, nannte man früher in der deutschen Sprache auch serbische Woiwodschaft. Ebenso abgeleitet ist der Name Vojvodina, einer Region Serbiens.", null);
        Tokeniser tokeniser = new TokeniserGermanStanford();
        String expected = "{\"annotatedTokens\":[{\"attributes\":{\"form\":\"Weitere\"},\"filtered\":false,\"start\":0,\"end\":7},{\"attributes\":{\"form\":\"Schreibweise\"},\"filtered\":false,\"start\":8,\"end\":20},{\"attributes\":{\"form\":\"sind\"},\"filtered\":false,\"start\":21,\"end\":25},{\"attributes\":{\"form\":\"Wojwode\"},\"filtered\":false,\"start\":26,\"end\":33},{\"attributes\":{\"form\":\"/\"},\"filtered\":false,\"start\":33,\"end\":34},{\"attributes\":{\"form\":\"Vojvode\"},\"filtered\":false,\"start\":34,\"end\":41},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":41,\"end\":42},{\"attributes\":{\"form\":\"Wojwoda\"},\"filtered\":false,\"start\":43,\"end\":50},{\"attributes\":{\"form\":\"/\"},\"filtered\":false,\"start\":50,\"end\":51},{\"attributes\":{\"form\":\"Vojvoda\"},\"filtered\":false,\"start\":51,\"end\":58},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":58,\"end\":59},{\"attributes\":{\"form\":\"Woiwod\"},\"filtered\":false,\"start\":60,\"end\":66},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":66,\"end\":67},{\"attributes\":{\"form\":\"Воевода\"},\"filtered\":false,\"start\":68,\"end\":75},{\"attributes\":{\"form\":\"/\"},\"filtered\":false,\"start\":75,\"end\":76},{\"attributes\":{\"form\":\"Wojewoda\"},\"filtered\":false,\"start\":76,\"end\":84},{\"attributes\":{\"form\":\"und\"},\"filtered\":false,\"start\":85,\"end\":88},{\"attributes\":{\"form\":\"Войвода\"},\"filtered\":false,\"start\":89,\"end\":96},{\"attributes\":{\"form\":\"/\"},\"filtered\":false,\"start\":96,\"end\":97},{\"attributes\":{\"form\":\"Wojwoda\"},\"filtered\":false,\"start\":97,\"end\":104},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":104,\"end\":105},{\"attributes\":{\"form\":\"hergeleitet\"},\"filtered\":false,\"start\":106,\"end\":117},{\"attributes\":{\"form\":\"aus\"},\"filtered\":false,\"start\":118,\"end\":121},{\"attributes\":{\"form\":\"dem\"},\"filtered\":false,\"start\":122,\"end\":125},{\"attributes\":{\"form\":\"Slawischen\"},\"filtered\":false,\"start\":126,\"end\":136},{\"attributes\":{\"form\":\"Войвода\"},\"filtered\":false,\"start\":137,\"end\":144},{\"attributes\":{\"form\":\"/\"},\"filtered\":false,\"start\":144,\"end\":145},{\"attributes\":{\"form\":\"Wojwoda\"},\"filtered\":false,\"start\":145,\"end\":152},{\"attributes\":{\"form\":\".\"},\"filtered\":false,\"start\":152,\"end\":153},{\"attributes\":{\"form\":\"Die\"},\"filtered\":false,\"start\":154,\"end\":157},{\"attributes\":{\"form\":\"Bezeichnung\"},\"filtered\":false,\"start\":158,\"end\":169},{\"attributes\":{\"form\":\"Woiwodschaft\"},\"filtered\":false,\"start\":170,\"end\":182},{\"attributes\":{\"form\":\"ist\"},\"filtered\":false,\"start\":183,\"end\":186},{\"attributes\":{\"form\":\"von\"},\"filtered\":false,\"start\":187,\"end\":190},{\"attributes\":{\"form\":\"Woiwode\"},\"filtered\":false,\"start\":191,\"end\":198},{\"attributes\":{\"form\":\"abgeleitet\"},\"filtered\":false,\"start\":199,\"end\":209},{\"attributes\":{\"form\":\".\"},\"filtered\":false,\"start\":209,\"end\":210},{\"attributes\":{\"form\":\"Die\"},\"filtered\":false,\"start\":211,\"end\":214},{\"attributes\":{\"form\":\"Region\"},\"filtered\":false,\"start\":215,\"end\":221},{\"attributes\":{\"form\":\"Banat\"},\"filtered\":false,\"start\":222,\"end\":227},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":227,\"end\":228},{\"attributes\":{\"form\":\"die\"},\"filtered\":false,\"start\":229,\"end\":232},{\"attributes\":{\"form\":\"heute\"},\"filtered\":false,\"start\":233,\"end\":238},{\"attributes\":{\"form\":\"in\"},\"filtered\":false,\"start\":239,\"end\":241},{\"attributes\":{\"form\":\"Serbien\"},\"filtered\":false,\"start\":242,\"end\":249},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":249,\"end\":250},{\"attributes\":{\"form\":\"Ungarn\"},\"filtered\":false,\"start\":251,\"end\":257},{\"attributes\":{\"form\":\"und\"},\"filtered\":false,\"start\":258,\"end\":261},{\"attributes\":{\"form\":\"Rumänien\"},\"filtered\":false,\"start\":262,\"end\":270},{\"attributes\":{\"form\":\"liegt\"},\"filtered\":false,\"start\":271,\"end\":276},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":276,\"end\":277},{\"attributes\":{\"form\":\"nannte\"},\"filtered\":false,\"start\":278,\"end\":284},{\"attributes\":{\"form\":\"man\"},\"filtered\":false,\"start\":285,\"end\":288},{\"attributes\":{\"form\":\"früher\"},\"filtered\":false,\"start\":289,\"end\":295},{\"attributes\":{\"form\":\"in\"},\"filtered\":false,\"start\":296,\"end\":298},{\"attributes\":{\"form\":\"der\"},\"filtered\":false,\"start\":299,\"end\":302},{\"attributes\":{\"form\":\"deutschen\"},\"filtered\":false,\"start\":303,\"end\":312},{\"attributes\":{\"form\":\"Sprache\"},\"filtered\":false,\"start\":313,\"end\":320},{\"attributes\":{\"form\":\"auch\"},\"filtered\":false,\"start\":321,\"end\":325},{\"attributes\":{\"form\":\"serbische\"},\"filtered\":false,\"start\":326,\"end\":335},{\"attributes\":{\"form\":\"Woiwodschaft\"},\"filtered\":false,\"start\":336,\"end\":348},{\"attributes\":{\"form\":\".\"},\"filtered\":false,\"start\":348,\"end\":349},{\"attributes\":{\"form\":\"Ebenso\"},\"filtered\":false,\"start\":350,\"end\":356},{\"attributes\":{\"form\":\"abgeleitet\"},\"filtered\":false,\"start\":357,\"end\":367},{\"attributes\":{\"form\":\"ist\"},\"filtered\":false,\"start\":368,\"end\":371},{\"attributes\":{\"form\":\"der\"},\"filtered\":false,\"start\":372,\"end\":375},{\"attributes\":{\"form\":\"Name\"},\"filtered\":false,\"start\":376,\"end\":380},{\"attributes\":{\"form\":\"Vojvodina\"},\"filtered\":false,\"start\":381,\"end\":390},{\"attributes\":{\"form\":\",\"},\"filtered\":false,\"start\":390,\"end\":391},{\"attributes\":{\"form\":\"einer\"},\"filtered\":false,\"start\":392,\"end\":397},{\"attributes\":{\"form\":\"Region\"},\"filtered\":false,\"start\":398,\"end\":404},{\"attributes\":{\"form\":\"Serbiens\"},\"filtered\":false,\"start\":405,\"end\":413},{\"attributes\":{\"form\":\".\"},\"filtered\":false,\"start\":413,\"end\":414}],\"attributes\":{},\"source\":{\"label\":\"\",\"text\":\"Weitere Schreibweise sind Wojwode/Vojvode, Wojwoda/Vojvoda, Woiwod, Воевода/Wojewoda und Войвода/Wojwoda, hergeleitet aus dem Slawischen Войвода/Wojwoda. Die Bezeichnung Woiwodschaft ist von Woiwode abgeleitet. Die Region Banat, die heute in Serbien, Ungarn und Rumänien liegt, nannte man früher in der deutschen Sprache auch serbische Woiwodschaft. Ebenso abgeleitet ist der Name Vojvodina, einer Region Serbiens.\"}}";
        Assert.assertEquals(expected, tokeniser.tokenise(i).toJson());
    }

}