package forge.ai.simulation;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import forge.GuiBase;
import forge.GuiDesktop;
import forge.StaticData;
import forge.ai.LobbyPlayerAi;
import forge.ai.simulation.GameStateEvaluator.Score;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import junit.framework.TestCase;

public class SimulationTestCase extends TestCase {
    private static boolean initialized = false;

    protected Game initAndCreateGame() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p2", null)));
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p1", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);

        if (!initialized) {
            GuiBase.setInterface(new GuiDesktop());
            FModel.initialize(null, new Function<ForgePreferences, Void>()  {
                @Override
                public Void apply(ForgePreferences preferences) {
                    preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, true);
                    return null;
                }
            });
            initialized = true;
        }
        return game;
    }

    protected GameSimulator createSimulator(Game game, Player p) {
        return new GameSimulator(new SimulationController(new Score(0)) {
            @Override
            public boolean shouldRecurse() {
                return false;
            }
        }, game, p);
    }

    protected Card findCardWithName(Game game, String name) {
        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
    
    protected String gameStateToString(Game game) {
        StringBuilder sb = new StringBuilder();
        for (ZoneType zone : ZoneType.values()) {
            CardCollectionView cards = game.getCardsIn(zone);
            if (!cards.isEmpty()) {
                sb.append("Zone ").append(zone.name()).append(":\n");
                for (Card c : game.getCardsIn(zone)) {
                    sb.append("  ").append(c).append("\n");
                }
            }
        }
        return sb.toString();
    }

    protected SpellAbility findSAWithPrefix(Card c, String prefix) {
        return findSAWithPrefix(c.getSpellAbilities(), prefix);
    }
    
    protected SpellAbility findSAWithPrefix(Iterable<SpellAbility> abilities, String prefix) {
        for (SpellAbility sa : abilities) {
            if (sa.getDescription().startsWith(prefix)) {
                return sa;
            }
        }
        return null;
    }

    protected Card createCard(String name, Player p) {
        IPaperCard paperCard = FModel.getMagicDb().getCommonCards().getCard(name);
        if (paperCard == null) {
            StaticData.instance().attemptToLoadCard(name, "");
            paperCard = FModel.getMagicDb().getCommonCards().getCard(name);
        }
        return Card.fromPaperCard(paperCard, p);
    }

    protected Card addCardToZone(String name, Player p, ZoneType zone) {
        Card c = createCard(name, p);
        p.getZone(zone).add(c);
        return c;
    }

    protected Card addCard(String name, Player p) {
        return addCardToZone(name, p, ZoneType.Battlefield);
    }
}
