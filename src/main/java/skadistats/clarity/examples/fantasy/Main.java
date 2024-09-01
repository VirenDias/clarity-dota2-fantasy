package skadistats.clarity.examples.fantasy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.io.Util;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.TextTable;

import java.io.IOException;

@UsesEntities
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public static void main(String[] args) throws Exception {
        new Main(args[0]).printFantasyStats();
    }

    private final ControllableRunner runner;

    public Main(String fileName) throws IOException, InterruptedException {
        runner = new ControllableRunner(new MappedFileSource(fileName)).runWith(this);
        runner.seek(runner.getLastTick());
        runner.halt();
    }
	
	private void printFantasyStats() {
		String data = "";
		data += "player_id,";
		data += "kills,";
		data += "deaths,";
		data += "creep_score,";
		data += "gpm,";
		data += "neutral_tokens_found,";
		data += "tower_kills,";
		data += "wards_placed,";
		data += "camps_stacked,";
		data += "runes_grabbed,";
		data += "watchers_taken,";
		data += "smokes_used,";
		data += "lotuses_grabbed,";
		data += "roshan_kills,";
		data += "teamfight_participation,";
		data += "stuns,";
		data += "tormentor_kills,";
		data += "first_blood,";
		data += "courier_kills\n";
		 
		float startTime = getEntity("CDOTAGamerulesProxy").getProperty("m_pGameRules.m_flGameStartTime");
		float endTime = getEntity("CDOTAGamerulesProxy").getProperty("m_pGameRules.m_flGameEndTime");
		float duration = (endTime - startTime) / 60;
		
        int team = 0;
        int pos = 0;

        for (int idx = 0; idx < 256; idx++) {
            try {
                int newTeam = new DefaultResolver<Integer>("CDOTA_PlayerResource", "m_vecPlayerData.%i.m_iPlayerTeam").resolveValue(idx, team, pos);
                if (newTeam != team) {
                    team = newTeam;
                    pos = 0;
                } else {
                    pos++;
                }
            } catch (Exception e) {
                // when the team resolver throws an exception, this was the last index there was
                break;
            }
            if (team != 2 && team != 3) {
                continue;
            }
            
            Object stats[] = {
            	new DefaultResolver<Long>("CDOTA_PlayerResource", "m_vecPlayerData.%i.m_iPlayerSteamID").resolveValue(idx, team, pos) - 76561197960265728L,
            	new DefaultResolver<Integer>("CDOTA_PlayerResource", "m_vecPlayerTeamData.%i.m_iKills").resolveValue(idx, team, pos),
            	new DefaultResolver<Integer>("CDOTA_PlayerResource", "m_vecPlayerTeamData.%i.m_iDeaths").resolveValue(idx, team, pos),
            	new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iLastHitCount").resolveValue(idx, team, pos) + new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iDenyCount").resolveValue(idx, team, pos),
            	new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iTotalEarnedGold").resolveValue(idx, team, pos) / duration,
            	new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iNeutralTokensFound").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iTowerKills").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iObserverWardsPlaced").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iCampsStacked").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iRunePickups").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iWatchersTaken").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iSmokesUsed").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iLotusesTaken").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iRoshanKills").resolveValue(idx, team, pos),
			    new DefaultResolver<Double>("CDOTA_PlayerResource", "m_vecPlayerTeamData.%i.m_flTeamFightParticipation").resolveValue(idx, team, pos),
			    new DefaultResolver<Double>("CDOTA_Data%n", "m_vecDataTeam.%p.m_fStuns").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iTormentorKills").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_PlayerResource", "m_vecPlayerTeamData.%i.m_iFirstBloodClaimed").resolveValue(idx, team, pos),
			    new DefaultResolver<Integer>("CDOTA_Data%n", "m_vecDataTeam.%p.m_iCourierKills").resolveValue(idx, team, pos)
            };
            
            for (int s = 0; s < stats.length; s++) {
            	data += stats[s];
            	data += (s == stats.length-1) ? "\n" : ",";
            }
        }

        System.out.println(data);
	}

    private String getTeamName(int team) {
        switch(team) {
            case 2:
                return "Radiant";
            case 3:
                return "Dire";
            default:
                return "";
        }
    }

    private Entity getEntity(String entityName) {
        return runner.getContext().getProcessor(Entities.class).getByDtName(entityName);
    }

    private interface ValueResolver<V> {
        V resolveValue(int index, int team, int pos);
    }

    private class DefaultResolver<V> implements ValueResolver<V> {
        private final String entityName;
        private final String pattern;

        public DefaultResolver(String entityName, String pattern) {
            this.entityName = entityName;
            this.pattern = pattern;
        }

        @Override
        public V resolveValue(int index, int team, int pos) {
            String fieldPathString = pattern
                    .replaceAll("%i", Util.arrayIdxToString(index))
                    .replaceAll("%t", Util.arrayIdxToString(team))
                    .replaceAll("%p", Util.arrayIdxToString(pos));
            String compiledName = entityName.replaceAll("%n", getTeamName(team));
            Entity entity = getEntity(compiledName);
            FieldPath fieldPath = entity.getDtClass().getFieldPathForName(fieldPathString);
            return entity.getPropertyForFieldPath(fieldPath);
        }
    }

}
