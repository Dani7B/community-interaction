package hbase.query.subquery;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import hbase.HBaseClient;
import hbase.query.AtLeast;
import hbase.query.Author;
import hbase.query.Authors;
import hbase.query.HQuery;
import hbase.query.Mention;
import hbase.query.time.TimeRange;

/**
 * Subquery to represent the authors-that-mentioned request
 * @author Daniele Morgantini
 */
public class AuthorsThatMentioned extends HSubQuery {
	
	private HBaseClient client;
	
	private TimeRange timeRange;
	
	private AtLeast atLeast;
	
	private List<Mention> mentions;
	
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
	
	
	/**
	 * Creates an instance of AuthorsThatMentioned subquery
	 * @return an instance of AuthorsThatMentioned subquery
	 * @param query the belonging query
	 * @param client the HBaseClient to query HBase
	 * @param timeRange the time window to take into account
	 * @param atLeast the minimum number of authors to mention
	 * @param mentions the mentions of authors
	 */
	public AuthorsThatMentioned(final HQuery query, final HBaseClient client, final TimeRange timeRange,
									final AtLeast atLeast, final Mention...mentions) {
		super(query);
		this.client = client;
		this.timeRange = timeRange;
		this.atLeast = atLeast;
		this.mentions = new ArrayList<Mention>();
		for(Mention m : mentions)
			this.mentions.add(m);
	}
	
	@Override
	public void execute(final Authors authors) throws IOException {
		
		Map<byte[],Integer> map = new HashMap<byte[],Integer>();
		long lowerBound = this.timeRange.getStart();
		long upperBound = this.timeRange.getEnd();
		int mentionMin = this.atLeast.getLowerBound();
		
				
		for(Mention m : this.mentions){
			
			String lowerRow = generateRowKey(m.getMentioned().getId(), lowerBound);
			String upperRow = generateRowKey(m.getMentioned().getId(), upperBound);
						
			Result[] results = this.client.scan(Bytes.toBytes(lowerRow),Bytes.toBytes(upperRow),
												Bytes.toBytes(String.valueOf(lowerBound)),
												Bytes.toBytes(String.valueOf(upperBound)));
			
			Set<byte[]> singleIDs = new HashSet<byte[]>();
			for(Result res : results){
				for(KeyValue kv : res.raw()){
					singleIDs.add(kv.getValue());
				}
			}
			
			for(byte[] id : singleIDs){
				int value = 1;
				if(map.containsKey(id)) {
					value = map.get(id) + 1;
				}
				map.put(id, value);
			}
		}
		
		List<Author> result = new ArrayList<Author>();
		for(Map.Entry<byte[], Integer> e : map.entrySet()) {
			int value = e.getValue();
			if(value >= mentionMin)
				result.add(new Author(Bytes.toLong(e.getKey())));
		}
		
		this.getQuery().updateUsers(result);
	}

	private static String generateRowKey(final long id, final long timestamp) {
		
		return id + "_" + dateFormatter.format(new Date(timestamp));
	}
	
}
