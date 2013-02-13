package quests;

import lineage2.commons.util.Rnd;
import lineage2.gameserver.model.Player;
import lineage2.gameserver.model.instances.NpcInstance;
import lineage2.gameserver.model.quest.Quest;
import lineage2.gameserver.model.quest.QuestState;
import lineage2.gameserver.scripts.Functions;
import lineage2.gameserver.scripts.ScriptFile;
import lineage2.gameserver.utils.Location;

public class _465_WeAreFriends extends Quest
  implements ScriptFile
{
  public static final int FEYA_STARTER = 32921;
  public static final int COCON = 33147;
  public static final int HUGE_COCON = 33148;
  public static final int SIGN_OF_GRATITUDE = 17377;
  private static NpcInstance npcFeya = null;

  public _465_WeAreFriends()
  {
    super(true);
    addStartNpc(32921);
    addTalkId(new int[] { 32922 });
    addFirstTalkId(new int[] { 32922 });
    addKillId(new int[] { 33147 });
    addKillId(new int[] { 33148 });
    addQuestItem(new int[] { 17377 });
    addLevelCheck(90, 99);
  }
  @Override
public void onShutdown() {
  }

  @Override
public String onEvent(String event, QuestState st, NpcInstance npc) {
    st.getPlayer();
    if (event.equalsIgnoreCase("32921-4.htm"))
    {
      st.setCond(1);
      st.setState(2);
      st.playSound("ItemSound.quest_accept");
    }
    if (event.equalsIgnoreCase("32922-4.htm"))
    {
      st.setCond(2);
      st.giveItems(17377, 2L);
      return "despawn_task";
    }

    if (event.equalsIgnoreCase("despawn_task"))
    {
      if (npcFeya == null)
        return null;
      st.unset("q465feya");
      npcFeya.deleteMe();
      npcFeya = null;
      return null;
    }

    if (event.equalsIgnoreCase("32921-8.htm"))
    {
      st.takeItems(17377, 2L);
      return "reward";
    }

    if (event.equalsIgnoreCase("32921-10.htm"))
    {
      return "reward";
    }

    if (event.equalsIgnoreCase("reward"))
    {
      st.giveItems(17378, 1L);
      st.unset("cond");
      st.playSound("ItemSound.quest_finish");
      st.exitCurrentQuest(this);

      if (st.getQuestItemsCount(17377) > 0L)
      {
        st.giveItems(30384, 2L);
        return "32921-10.htm";
      }

      st.giveItems(30384, 4L);
      return "32921-8.htm";
    }

    return event;
  }

  @Override
public String onFirstTalk(NpcInstance npc, Player player)
  {
    QuestState st = player.getQuestState(getClass());
    if (st == null)
      return "32922.htm";
    if ((st.get("q465feya") != null) && (Integer.parseInt(st.get("q465feya")) != npc.getObjectId()))
      return "32922-1.htm";
    if (st.get("q465feya") == null)
      return "32922-1.htm";
    return "32922-3.htm";
  }

  @Override
public String onTalk(NpcInstance npc, QuestState st)
  {
    Player player = st.getPlayer();
    int npcId = npc.getNpcId();
    int state = st.getState();
    int cond = st.getCond();
    if (npcId == 32921)
    {
      if (state == 1)
      {
        if (player.getLevel() < 90)
          return "32921-lvl.htm";
        if (!st.isNowAvailableByTime())
          return "32921-comp.htm";
        if (st.getPlayer().getLevel() < 90)
          return "32921-lvl.htm";
        return "32921.htm";
      }
      if (state == 2)
      {
        if (cond == 1) {
          return "32921-5.htm";
        }
        if (cond == 2)
        {
          return "32921-6.htm";
        }
      }
    }
    return "noquest";
  }
  @Override
public void onLoad() {
  }
  @Override
public void onReload() {  } 
  @Override
public String onKill(NpcInstance npc, QuestState st) { if (Rnd.chance(5))
    {
      npcFeya = Functions.spawn(Location.findPointToStay(st.getPlayer(), 50, 100), 32922);
      st.set("q465feya", "" + npcFeya.getObjectId() + "");
      st.startQuestTimer("despawn_task", 180000L);
    }
    return null;
  }
}