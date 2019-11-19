package org.insightcentre.saffron.web.mongodb;

import com.github.mongobee.Mongobee;
import com.github.mongobee.exception.MongobeeException;

/**
 * Performs database migrations required to upgrade Saffron
 * 
 * @author Bianca Pereira
 *
 */
public class MongoDBMigrations {

	public static void main(String[] args) throws MongobeeException {
		MongoDBHandler mongoHandler = new MongoDBHandler();
		
		//Expected URL: mongodb://[username:password@]host1[:port1][,host2[:port2],...[,hostN[:portN]]][/[database[.collection]][?options]]
		Mongobee runner = new Mongobee(mongoHandler.getMongoUrl());
		runner.setChangeLogsScanPackage("org.insightcentre.saffron.web.mongodb.migration");

		runner.execute();
	}
}
