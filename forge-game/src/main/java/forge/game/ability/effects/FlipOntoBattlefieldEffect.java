package forge.game.ability.effects;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Localizer;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FlipOntoBattlefieldEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        // Basic parameters defining the chances
        final float chanceToFlip = 0.85f;
        final int maxFlipTimes = 2;
        final float chanceToHit = 0.70f;
        final float chanceToHitTwoCards = 0.20f;

        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = host.getGame();
        boolean flippedOnce = false;

        // TODO: allow to make a bounding box of sorts somehow, ideally - upgrade to a full system allowing to actually target by location
        CardCollectionView tgtBox = p.getController().chooseCardsForEffect(game.getCardsIn(ZoneType.Battlefield), sa, Localizer.getInstance().getMessage("lblChooseDesiredLocation"), 1, 1, sa.hasParam("AllowRandom"), null);

        Card tgtLoc = tgtBox.getFirst();

        Card lhsNeighbor = getNeighboringCard(tgtLoc, -1);
        Card rhsNeighbor = getNeighboringCard(tgtLoc, 1);

        CardCollection randChoices = new CardCollection();
        randChoices.add(tgtLoc);
        if (lhsNeighbor != null) {
            randChoices.add(lhsNeighbor);
        } else if (rhsNeighbor != null) {
            randChoices.add(rhsNeighbor);
        }

        // TODO: would be fun to add a small chance (e.g. 3-5%) to land unpredictably on some random target?

        flippedOnce = MyRandom.getRandom().nextFloat() <= chanceToFlip; // 20% chance that the card won't flip even once
        if (!flippedOnce) {
            sa.setSVar("TimesFlipped", "0");
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblDidNotFlipOver"), null);
            return;
        } else {
            int flippedTimes = MyRandom.getRandom().nextInt(maxFlipTimes) + 1;
            sa.setSVar("TimesFlipped", String.valueOf(flippedTimes)); // Currently the exact # of times is unused
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblFlippedOver", flippedTimes), null);
        }

        // Choose what was hit
        CardCollection hit = new CardCollection();
        float outcome = MyRandom.getRandom().nextFloat();
        if (outcome <= chanceToHitTwoCards) {
            hit.addAll(Aggregates.random(randChoices, randChoices.size() > 1 ? 2 : 1));
            if (hit.size() == 2) {
                game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblLandedOnTwoCards", hit.getFirst(), hit.getLast()), null);
            } else {
                game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblLandedOnOneCard", hit.getFirst()), null);
            }
        }
        else if (outcome <= chanceToHit) {
            hit.add(Aggregates.random(randChoices));
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblLandedOnOneCard", hit.getFirst()), null);
        } else {
            game.getAction().notifyOfValue(sa, host, Localizer.getInstance().getMessage("lblDidNotLandOnCards"), null);
        }

        // Remember whatever was hit
        for (Card c : hit) {
            host.addRemembered(c);
        }
    }

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final Card host = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();

        sb.append("Flip ");
        sb.append(host.toString());
        sb.append(" onto the battlefield from a height of at least one foot.");

        return sb.toString();
    }

    private Card getNeighboringCard(Card c, int direction) {
        // Currently gets the nearest (by ID) card to the left or to the right of the designated one by type
        Player controller = c.getController();
        ArrayList<Card> cardsOTB = Lists.newArrayList(CardLists.filter(
                controller.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                    @Override
                    public boolean apply(Card card) {
                        if (c.isCreature()) {
                            return card.isCreature();
                        } else if (c.isPlaneswalker() || c.isArtifact() || (c.isEnchantment() && !c.isAura())) {
                            return card.isPlaneswalker() || card.isArtifact() || (c.isEnchantment() && !c.isAura());
                        } else if (c.isLand()) {
                            return card.isLand();
                        } else if (c.isAttachedToEntity()) {
                            return card.isAttachedToEntity(c.getEntityAttachedTo()) || c.equals(card.getAttachedTo());
                        }
                        return card.sharesCardTypeWith(c);
                    }
                }
        ));

        Collections.sort(cardsOTB, new Comparator<Card>() {
            @Override
            public int compare(Card c0, Card c1) {
                return c0.getId() - c1.getId();
            }
        });

        int loc = cardsOTB.indexOf(c);
        if (direction < 0 && loc > 0) {
            return cardsOTB.get(loc - 1);
        } else if (loc < cardsOTB.size() - 1) {
            return cardsOTB.get(loc + 1);
        }

        return c;
    }
}
