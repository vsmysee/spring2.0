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

package org.springframework.jdbc.object;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;

/**
 * RdbmsOperation using a JdbcTemplate and representing a SQL-based
 * operation such as a query or update, as opposed to a stored procedure.
 *
 * <p>Configures a PreparedStatementCreatorFactory based on the
 * declared parameters.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see org.springframework.jdbc.core.PreparedStatementCreatorFactory
 */
public abstract class SqlOperation extends RdbmsOperation {

	/**
	 * Object enabling us to create PreparedStatementCreators
	 * efficiently, based on this class's declared parameters.
	 */
	private PreparedStatementCreatorFactory preparedStatementFactory;


	/**
	 * Overridden method to configure the PreparedStatementCreatorFactory
	 * based on our declared parameters.
	 */
	protected final void compileInternal() {
		// Validate parameter count.
		int bindVarCount = NamedParameterUtils.countParameterPlaceholders(getSql());
		if (bindVarCount != getDeclaredParameters().size()) {
			throw new InvalidDataAccessApiUsageException(
					"SQL '" + getSql() + "' requires " + bindVarCount + " bind variables, but " +
					getDeclaredParameters().size() + " variables were declared for this object");
		}

		this.preparedStatementFactory = new PreparedStatementCreatorFactory(getSql(), getDeclaredParameters());
		this.preparedStatementFactory.setResultSetType(getResultSetType());
		this.preparedStatementFactory.setUpdatableResults(isUpdatableResults());
		this.preparedStatementFactory.setReturnGeneratedKeys(isReturnGeneratedKeys());
		if (getGeneratedKeysColumnNames() != null) {
			this.preparedStatementFactory.setGeneratedKeysColumnNames(getGeneratedKeysColumnNames());
		}
		this.preparedStatementFactory.setNativeJdbcExtractor(getJdbcTemplate().getNativeJdbcExtractor());
		this.preparedStatementFactory.setSqlToUse(NamedParameterUtils.parseSqlStatementIntoString(getSql()));

		onCompileInternal();
	}

	/**
	 * Hook method that subclasses may override to post-process compilation.
	 * This implementation does nothing.
	 * @see #compileInternal
	 */
	protected void onCompileInternal() {
	}


	/**
	 * Return a PreparedStatementSetter to perform an operation
	 * with the given parameters.
	 * @param params parameter array. May be <code>null</code>.
	 */
	protected final PreparedStatementSetter newPreparedStatementSetter(Object[] params) {
		return this.preparedStatementFactory.newPreparedStatementSetter(params);
	}

	/**
	 * Return a PreparedStatementCreator to perform an operation
	 * with the given parameters.
	 * @param params parameter array. May be <code>null</code>.
	 */
	protected final PreparedStatementCreator newPreparedStatementCreator(Object[] params) {
		return this.preparedStatementFactory.newPreparedStatementCreator(params);
	}

	/**
	 * Return a PreparedStatementCreator to perform an operation
	 * with the given parameters.
	 * @param sqlToUse the actual SQL statement to use (if different from
	 * the factory's, for example because of named parameter expanding)
	 * @param params parameter array. May be <code>null</code>.
	 */
	protected final PreparedStatementCreator newPreparedStatementCreator(String sqlToUse, Object[] params) {
		return this.preparedStatementFactory.newPreparedStatementCreator(sqlToUse, params);
	}

}
