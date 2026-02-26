package structures.basic;

import java.util.HashSet;
import java.util.Set;

import utils.BasicObjectBuilders;
import utils.StaticConfFiles;

public class BetterUnit extends Unit {

Set<String> keywords;
private boolean openingGambitTriggered = false;

public BetterUnit() {
keywords = new HashSet<>();
}

public BetterUnit(Set<String> keywords) {
super();
this.keywords = keywords != null ? keywords : new HashSet<>();
}

public Set<String> getKeywords() {
return keywords;
}

public void setKeywords(Set<String> keywords) {
this.keywords = keywords;
};

// SC-18: check if the unit has a given keyword
private boolean hasKeyword(String keyword) {
return keywords.contains(keyword);
}

// SC-18: Opening Gambit triggers only once on summon
public boolean triggerOpeningGambit() {
if (!openingGambitTriggered && hasKeyword("OpeningGambit")) {
openingGambitTriggered = true;
return true;
}
return false;
}

// SC-18: Provoke – enforced during movement validation
public boolean hasProvoke() {
return hasKeyword("Provoke");
}

// SC-18: Rush keyword
public boolean hasRush() {
return hasKeyword("Rush");
}

// SC-18: Deathwatch triggers when any unit dies
public boolean triggerDeathwatch() {
return hasKeyword("Deathwatch");
}


public static void main(String[] args) {

BetterUnit unit = (BetterUnit)BasicObjectBuilders.loadUnit(StaticConfFiles.humanAvatar, 0, BetterUnit.class);
Set<String> keywords = new HashSet<String>();
keywords.add("MyKeyword");
unit.setKeywords(keywords);

System.err.println(unit.getClass());

}
}
