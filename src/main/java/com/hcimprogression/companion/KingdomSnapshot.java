package com.hcimprogression.companion;

/** Last Kingdom of Miscellania state observed by the RuneLite client. */
public class KingdomSnapshot
{
    private final boolean unlocked;
    private final boolean royalTroubleComplete;
    private final boolean observed;
    private final int coffer;
    private final int approval;
    private final long observedAt;

    public KingdomSnapshot(boolean unlocked, boolean royalTroubleComplete, boolean observed,
        int coffer, int approval, long observedAt)
    {
        this.unlocked = unlocked;
        this.royalTroubleComplete = royalTroubleComplete;
        this.observed = observed;
        this.coffer = coffer;
        this.approval = approval;
        this.observedAt = observedAt;
    }

    public boolean isUnlocked() { return unlocked; }
    public boolean isRoyalTroubleComplete() { return royalTroubleComplete; }
    public boolean isObserved() { return observed; }
    public int getCoffer() { return coffer; }
    public int getApproval() { return approval; }
    public long getObservedAt() { return observedAt; }
}
