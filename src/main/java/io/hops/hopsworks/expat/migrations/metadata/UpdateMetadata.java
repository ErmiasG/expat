/**
 * This file is part of Expat
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Expat is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Expat is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 */
package io.hops.hopsworks.expat.migrations.metadata;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.expat.migrations.MigrateStep;
import io.hops.hopsworks.expat.migrations.MigrationException;
import io.hops.hopsworks.expat.migrations.RollbackException;
import io.hops.hopsworks.expat.migrations.projects.provenance.HopsClient;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.Connection;

public class UpdateMetadata implements MigrateStep {
  private final static Logger LOGGER = LogManager.getLogger(UpdateMetadata.class);
  
  private static String newPath =
    Path.SEPARATOR + "user" + Path.SEPARATOR + "metadata" + Path.SEPARATOR + "uploads" + Path.SEPARATOR;
  private static String oldPath =
    Path.SEPARATOR + "Projects" + Path.SEPARATOR + "Uploads" + Path.SEPARATOR;
  protected Connection connection;
  
  @Override
  public void migrate() throws MigrationException {
    LOGGER.info("metadata migration");
    DistributedFileSystemOps dfso = null;
    try {
      dfso = HopsClient.getDFSO();
      move(dfso, oldPath, newPath);
      dfso.rm(new Path(oldPath), true);
    } catch (IllegalStateException | IOException e) {
      throw new MigrationException("error", e);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  @Override
  public void rollback() throws RollbackException {
    LOGGER.info("metadata rollback");
    DistributedFileSystemOps dfso = null;
    try {
      dfso = HopsClient.getDFSO();
      move(dfso, newPath, oldPath);
      dfso.rm(new Path(oldPath), true);
    } catch (IllegalStateException | IOException e) {
      throw new RollbackException("error", e);
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }
  
  private void move(DistributedFileSystemOps dfso, String src, String dst) throws IOException {
    LOGGER.info("metadata move from:{} to:{}", new Object[]{src, dst});
    if(!dfso.isDir(src)) {
      LOGGER.info("nothing to move at src:{}", src);
      return;
    }
    if(dfso.isDir(dst)) {
      throw new IllegalArgumentException("dst folder exists");
    }
    dfso.mkdirs(new Path(dst).getParent(), FsPermission.getDefault());
    dfso.moveWithinHdfs(new Path(src), new Path(dst));
  }
}