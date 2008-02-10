//$Id$
package org.hibernate.search.test.embedded;

import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.event.PostCollectionRecreateEventListener;
import org.hibernate.event.PostCollectionRemoveEventListener;
import org.hibernate.event.PostCollectionUpdateEventListener;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.event.FullTextIndexCollectionEventListener;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Emmanuel Bernard
 */
public class EmbeddedTest extends SearchTestCase {

	public void testEmbeddedIndexing() throws Exception {
		Tower tower = new Tower();
		tower.setName( "JBoss tower" );
		Address a = new Address();
		a.setStreet( "Tower place" );
		a.getTowers().add( tower );
		tower.setAddress( a );
		Owner o = new Owner();
		o.setName( "Atlanta Renting corp" );
		a.setOwnedBy( o );
		o.setAddress( a );
		Country c = new Country();
		c.setName( "France" );
		a.setCountry( c );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		tx.commit();


		FullTextSession session = Search.createFullTextSession( s );
		QueryParser parser = new QueryParser( "id", new StandardAnalyzer() );
		Query query;
		List result;

		query = parser.parse( "address.street:place" );
		result = session.createFullTextQuery( query ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = parser.parse( "address.ownedBy_name:renting" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property in embedded", 1, result.size() );

		query = parser.parse( "address.id:" + a.getId().toString() );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property by id of embedded", 1, result.size() );

		query = parser.parse( "address.country.name:" + a.getCountry().getName() );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "unable to find property with 2 levels of embedded", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		Address address = (Address) s.get( Address.class, a.getId() );
		address.getOwnedBy().setName( "Buckhead community" );
		tx.commit();


		s.clear();

		session = Search.createFullTextSession( s );

		query = parser.parse( "address.ownedBy_name:buckhead" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();

	}

	public void testContainedIn() throws Exception {
		Tower tower = new Tower();
		tower.setName( "JBoss tower" );
		Address a = new Address();
		a.setStreet( "Tower place" );
		a.getTowers().add( tower );
		tower.setAddress( a );
		Owner o = new Owner();
		o.setName( "Atlanta Renting corp" );
		a.setOwnedBy( o );
		o.setAddress( a );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( tower );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		Address address = (Address) s.get( Address.class, a.getId() );
		address.setStreet( "Peachtree Road NE" );
		tx.commit();

		s.clear();

		FullTextSession session = Search.createFullTextSession( s );
		QueryParser parser = new QueryParser( "id", new StandardAnalyzer() );
		Query query;
		List result;

		query = parser.parse( "address.street:peachtree" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "change in embedded not reflected in root index", 1, result.size() );

		s.clear();

		tx = s.beginTransaction();
		address = (Address) s.get( Address.class, a.getId() );
		Tower tower1 = address.getTowers().iterator().next();
		tower1.setAddress( null );
		address.getTowers().remove( tower1 );
		tx.commit();

		s.clear();

		session = Search.createFullTextSession( s );

		query = parser.parse( "address.street:peachtree" );
		result = session.createFullTextQuery( query, Tower.class ).list();
		assertEquals( "breaking link fails", 0, result.size() );

		tx = s.beginTransaction();
		s.delete( s.get( Tower.class, tower.getId() ) );
		tx.commit();

		s.close();

	}

	public void testIndexedEmbeddedAndCollections() throws Exception {
		Author a = new Author();
		a.setName( "Voltaire" );
		Author a2 = new Author();
		a2.setName( "Victor Hugo" );
		Author a3 = new Author();
		a3.setName( "Moliere" );
		Author a4 = new Author();
		a4.setName( "Proust" );

		Order o = new Order();
		o.setOrderNumber( "ACVBNM" );

		Order o2 = new Order();
		o2.setOrderNumber( "ZERTYD" );

		Product p1 = new Product();
		p1.setName( "Candide" );
		p1.getAuthors().add( a );
		p1.getAuthors().add( a2 ); //be creative

		Product p2 = new Product();
		p2.setName( "Le malade imaginaire" );
		p2.getAuthors().add( a3 );
		p2.getOrders().put( "Emmanuel", o );
		p2.getOrders().put( "Gavin", o2 );


		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( a );
		s.persist( a2 );
		s.persist( a3 );
		s.persist( a4 );
		s.persist( o );
		s.persist( o2 );
		s.persist( p1 );
		s.persist( p2 );
		tx.commit();

		s.clear();

		FullTextSession session = Search.createFullTextSession( s );
		tx = session.beginTransaction();

		QueryParser parser = new MultiFieldQueryParser( new String[] { "name", "authors.name" }, new StandardAnalyzer() );
		Query query;
		List result;

		query = parser.parse( "Hugo" );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "collection of embedded ignored", 1, result.size() );

		//update the collection
		Product p = (Product) result.get( 0 );
		p.getAuthors().add( a4 );

		//PhraseQuery
		query = new TermQuery( new Term( "orders.orderNumber", "ZERTYD" ) );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "collection of untokenized ignored", 1, result.size() );
		query = new TermQuery( new Term( "orders.orderNumber", "ACVBNM" ) );
		result = session.createFullTextQuery( query, Product.class ).list();
		assertEquals( "collection of untokenized ignored", 1, result.size() );

		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		session = Search.createFullTextSession( s );
		query = parser.parse( "Proust" );
		result = session.createFullTextQuery( query, Product.class ).list();
		//HSEARCH-56
		assertEquals( "update of collection of embedded ignored", 1, result.size() );

		s.delete( s.get( Product.class, p1.getId() ) );
		s.delete( s.get( Product.class, p2.getId() ) );
		tx.commit();
		s.close();

	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		FullTextIndexCollectionEventListener del = new FullTextIndexCollectionEventListener();
		//cfg.getEventListeners().setPostCollectionRecreateEventListeners( new PostCollectionRecreateEventListener[]{del} );
		//cfg.getEventListeners().setPostCollectionUpdateEventListeners( new PostCollectionUpdateEventListener[]{del} );
		//cfg.getEventListeners().setPostCollectionRemoveEventListeners( new PostCollectionRemoveEventListener[]{del} );
	}

	protected Class[] getMappings() {
		return new Class[] {
				Tower.class,
				Address.class,
				Product.class,
				Order.class,
				Author.class,
				Country.class
		};
	}
}
