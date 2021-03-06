/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 17.10.2005
 */
public class DataSourceJtaTransactionTests extends TestCase {

	public void testJtaTransactionCommit() throws Exception {
		doTestJtaTransaction(false);
	}

	public void testJtaTransactionRollback() throws Exception {
		doTestJtaTransaction(true);
	}

	private void doTestJtaTransaction(final boolean rollback) throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		if (rollback) {
			ut.rollback();
		}
		else {
			ut.commit();
		}
		utControl.setVoidCallable(1);
		utControl.replay();

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		con.close();
		conControl.setVoidCallable(1);
		conControl.replay();
		dsControl.replay();

		JtaTransactionManager ptm = new JtaTransactionManager(ut);
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Connection c = DataSourceUtils.getConnection(ds);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				DataSourceUtils.releaseConnection(c, ds);

				c = DataSourceUtils.getConnection(ds);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
				DataSourceUtils.releaseConnection(c, ds);

				if (rollback) {
					status.setRollbackOnly();
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		dsControl.verify();
		conControl.verify();
		utControl.verify();
	}

	public void testJtaTransactionCommitWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, false, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, true, true, false);
	}

	public void testJtaTransactionCommitWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(false, false, true, true);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNew() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, false, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithAccessAfterResume() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnection() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, false, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithOpenOuterConnectionAccessed() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, true, true, false);
	}

	public void testJtaTransactionRollbackWithPropagationRequiresNewWithTransactionAwareDataSource() throws Exception {
		doTestJtaTransactionWithPropagationRequiresNew(true, false, true, true);
	}

	private void doTestJtaTransactionWithPropagationRequiresNew(
			final boolean rollback, final boolean openOuterConnection, final boolean accessAfterResume,
			final boolean useTransactionAwareDataSource) throws Exception {

		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();
		MockControl tmControl = MockControl.createControl(TransactionManager.class);
		TransactionManager tm = (TransactionManager) tmControl.getMock();
		MockControl txControl = MockControl.createControl(Transaction.class);
		Transaction tx = (Transaction) txControl.getMock();
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_NO_TRANSACTION, 1);
		ut.begin();
		utControl.setVoidCallable(1);
		ut.getStatus();
		utControl.setReturnValue(Status.STATUS_ACTIVE, 11);
		tm.suspend();
		tmControl.setReturnValue(tx, 5);
		ut.begin();
		utControl.setVoidCallable(5);
		ut.commit();
		utControl.setVoidCallable(5);
		tm.resume(tx);
		tmControl.setVoidCallable(5);
		if (rollback) {
			ut.rollback();
		}
		else {
			ut.commit();
		}
		utControl.setVoidCallable(1);
		utControl.replay();
		tmControl.replay();

		final MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		final MockControl conControl = MockControl.createControl(Connection.class);
		final Connection con = (Connection) conControl.getMock();
		ds.getConnection();
		dsControl.setReturnValue(con, 1);
		if (!openOuterConnection) {
			con.close();
			conControl.setVoidCallable(1);
		}
		conControl.replay();
		dsControl.replay();

		final DataSource dsToUse = useTransactionAwareDataSource ?
				new TransactionAwareDataSourceProxy(ds) : ds;

		JtaTransactionManager ptm = new JtaTransactionManager(ut, tm);
		final TransactionTemplate tt = new TransactionTemplate(ptm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
				assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
				assertTrue("Is new transaction", status.isNewTransaction());

				Connection c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				DataSourceUtils.releaseConnection(c, dsToUse);

				c = DataSourceUtils.getConnection(dsToUse);
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
				if (!openOuterConnection) {
					DataSourceUtils.releaseConnection(c, dsToUse);
				}

				for (int i = 0; i < 5; i++) {

					tt.execute(new TransactionCallbackWithoutResult() {
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
							assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
							assertTrue("Is new transaction", status.isNewTransaction());

							try {
								dsControl.verify();
								conControl.verify();
								dsControl.reset();
								conControl.reset();
								ds.getConnection();
								dsControl.setReturnValue(con, 1);
								con.close();
								conControl.setVoidCallable(1);
								dsControl.replay();
								conControl.replay();
							}
							catch (SQLException ex) {
							}

							Connection c = DataSourceUtils.getConnection(dsToUse);
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
							DataSourceUtils.releaseConnection(c, dsToUse);

							c = DataSourceUtils.getConnection(dsToUse);
							assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
							DataSourceUtils.releaseConnection(c, dsToUse);
						}
					});

				}

				if (rollback) {
					status.setRollbackOnly();
				}

				if (accessAfterResume) {
					try {
						if (!openOuterConnection) {
							dsControl.verify();
							dsControl.reset();
							ds.getConnection();
							dsControl.setReturnValue(con, 1);
							dsControl.replay();
						}
						conControl.verify();
						conControl.reset();
						con.close();
						conControl.setVoidCallable(1);
						conControl.replay();
					}
					catch (SQLException ex) {
					}

					if (!openOuterConnection) {
						c = DataSourceUtils.getConnection(dsToUse);
					}
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
					DataSourceUtils.releaseConnection(c, dsToUse);

					c = DataSourceUtils.getConnection(dsToUse);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(dsToUse));
					DataSourceUtils.releaseConnection(c, dsToUse);
				}

				else {
					if (openOuterConnection) {
						try {
							conControl.verify();
							conControl.reset();
							con.close();
							conControl.setVoidCallable(1);
							conControl.replay();
						}
						catch (SQLException ex) {
						}
						DataSourceUtils.releaseConnection(c, dsToUse);
					}
				}
			}
		});

		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(dsToUse));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());
		dsControl.verify();
		conControl.verify();
		utControl.verify();
		tmControl.verify();
	}

	public void testJtaTransactionWithConnectionHolderStillBound() throws Exception {
		MockControl utControl = MockControl.createControl(UserTransaction.class);
		UserTransaction ut = (UserTransaction) utControl.getMock();

		MockControl dsControl = MockControl.createControl(DataSource.class);
		final DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		JtaTransactionManager ptm = new JtaTransactionManager(ut) {
			protected void doRegisterAfterCompletionWithJtaTransaction(
					JtaTransactionObject txObject, final List synchronizations) {
				Thread async = new Thread() {
					public void run() {
						invokeAfterCompletion(synchronizations, TransactionSynchronization.STATUS_COMMITTED);
					}
				};
				async.start();
				try {
					async.join();
				}
				catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
		};
		TransactionTemplate tt = new TransactionTemplate(ptm);
		assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
		assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

		for (int i = 0; i < 3; i++) {
			utControl.reset();
			ut.getStatus();
			utControl.setReturnValue(Status.STATUS_ACTIVE, 1);
			utControl.replay();

			dsControl.reset();
			conControl.reset();
			ds.getConnection();
			dsControl.setReturnValue(con, 1);
			con.close();
			conControl.setVoidCallable(1);
			dsControl.replay();
			conControl.replay();

			final boolean releaseCon = (i != 1);

			tt.execute(new TransactionCallbackWithoutResult() {
				protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
					assertTrue("JTA synchronizations active", TransactionSynchronizationManager.isSynchronizationActive());
					assertTrue("Is existing transaction", !status.isNewTransaction());

					Connection c = DataSourceUtils.getConnection(ds);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					DataSourceUtils.releaseConnection(c, ds);

					c = DataSourceUtils.getConnection(ds);
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(ds));
					if (releaseCon) {
						DataSourceUtils.releaseConnection(c, ds);
					}
				}
			});

			if (!releaseCon) {
				assertTrue("Still has connection holder", TransactionSynchronizationManager.hasResource(ds));
			}
			else {
				assertTrue("Hasn't thread connection", !TransactionSynchronizationManager.hasResource(ds));
			}
			assertTrue("JTA synchronizations not active", !TransactionSynchronizationManager.isSynchronizationActive());

			conControl.verify();
			dsControl.verify();
			utControl.verify();
		}
	}

	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}

}
