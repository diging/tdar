package org.tdar.core.service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tdar.core.bean.resource.ResourceType;
import org.tdar.core.bean.resource.VersionType;
import org.tdar.core.bean.statistics.AggregateStatistic;
import org.tdar.core.bean.statistics.AggregateStatistic.StatisticType;
import org.tdar.core.dao.StatisticDao;
import org.tdar.utils.Pair;

import com.ibm.icu.util.GregorianCalendar;

/**
 * Helper class for running statistics and working with @link AggregatedStatistic objects
 * 
 * @author abrin
 *
 */
@Service
public class StatisticService extends ServiceInterface.TypedDaoBase<AggregateStatistic, StatisticDao> {

    private final Date startDate = new GregorianCalendar(2008, 1, 1).getTime();

    /**
     * Get Total @link Resource Statistics (counts) Grouped by week
     * 
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Date, Map<StatisticType, Long>> getResourceStatistics() {
        List<StatisticType> types = Arrays.asList(StatisticType.NUM_CODING_SHEET, StatisticType.NUM_DATASET, StatisticType.NUM_DOCUMENT,
                StatisticType.NUM_IMAGE, StatisticType.NUM_ONTOLOGY, StatisticType.NUM_PROJECT, StatisticType.NUM_SENSORY_DATA, StatisticType.NUM_VIDEO,
                StatisticType.NUM_ARCHIVES, StatisticType.NUM_GIS);
        return getDao().getStatistics(startDate, new Date(), types.toArray(new StatisticType[0]));
    }

    /** 
     * Get total @link ResourceCollection Statistics (counts) grouped by week
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Date, Map<StatisticType, Long>> getCollectionStatistics() {
        List<StatisticType> types = Arrays.asList(StatisticType.NUM_COLLECTIONS);
        return getDao().getStatistics(startDate, new Date(), types.toArray(new StatisticType[0]));
    }

    /**
     * get real-time @link Resource Statistics
     * 
     * @return
     */
    @Transactional(readOnly = true)
    public Map<ResourceType, List<BigInteger>> getCurrentResourceStats() {
        return getDao().getCurrentResourceStats();
    }

    /**
     * Get current user statistics (grouped by week)
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Date, Map<StatisticType, Long>> getUserStatistics() {
        List<StatisticType> types = Arrays.asList(StatisticType.NUM_USERS, StatisticType.NUM_ACTUAL_CONTRIBUTORS);
        return getDao().getStatistics(startDate, new Date(), types.toArray(new StatisticType[0]));
    }

    /**
     * Get File Average Statistics (for pie chart) by extension
     * 
     * @param types
     * @return
     */
    @Transactional(readOnly = true)
    public Map<String, List<Number>> getFileAverageStats(List<VersionType> types) {
        return getDao().getFileAverageStats(types);
    }

    /**
     * Get the repository size grouped by week
     * @return
     */
    @Transactional(readOnly = true)
    public Map<Date, Map<StatisticType, Long>> getRepositorySizes() {
        List<StatisticType> types = Arrays.asList(StatisticType.REPOSITORY_SIZE);
        return getDao().getStatistics(startDate, new Date(), types.toArray(new StatisticType[0]));
    }

    /**
     * Get current @link Resource Counts limited to those with files (grouped by week)
     * @return
     */
    @Transactional(readOnly=true)
    public Map<Date, Map<StatisticType, Long>> getResourceStatisticsWithFiles() {
        List<StatisticType> types = Arrays.asList(StatisticType.NUM_CODING_SHEET_WITH_FILES, StatisticType.NUM_DATASET_WITH_FILES,
                StatisticType.NUM_DOCUMENT_WITH_FILES,
                StatisticType.NUM_IMAGE_WITH_FILES, StatisticType.NUM_ONTOLOGY_WITH_FILES, StatisticType.NUM_PROJECT,
                StatisticType.NUM_SENSORY_DATA_WITH_FILES, StatisticType.NUM_VIDEO_WITH_FILES,
                StatisticType.NUM_GIS_WITH_FILES, StatisticType.NUM_ARCHIVES_WITH_FILES);
        return getDao().getStatistics(startDate, new Date(), types.toArray(new StatisticType[0]));
    }

    /**
     * Get user Login stats (# of logins by # of users)
     * 
     * @return
     */
    @Transactional(readOnly=true)
    public List<Pair<Long, Long>> getUserLoginStats() {
        return getDao().getUserLoginStats();
    }

    @Transactional(readOnly=true)
    public  Map<String, Long>  getFileStats(List<VersionType> types) {
        return getDao().getFileStats(types);
    }
    
}
