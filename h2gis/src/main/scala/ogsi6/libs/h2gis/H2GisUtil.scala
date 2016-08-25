package ogsi6.libs.h2gis

import java.io.File
import javax.sql.DataSource

import org.apache.commons.dbcp.BasicDataSource
import org.h2.Driver
import org.h2gis.h2spatialext.CreateSpatialExtension

/**
  * Created by pappmar on 08/08/2016.
  */
object H2GisUtil {

  def createDataSource(dbFile: File) : (DataSource, () => Unit) = {
    val dbDir = dbFile.getParentFile
    val isNew = !dbDir.exists()
    //    dbDir.mkdirs()

    val basicDataSource: BasicDataSource = new BasicDataSource
    basicDataSource.setDriverClassLoader(getClass.getClassLoader)
    basicDataSource.setDriverClassName(classOf[Driver].getName)
    basicDataSource.setPoolPreparedStatements(false)
    basicDataSource.setUrl("jdbc:h2:" + dbFile.toURI.toURL.toExternalForm.replaceAllLiterally("\\", "/") + ";DATABASE_TO_UPPER=false")
    //    basicDataSource.setUrl("jdbc:h2:" + dbFile.toURI.toURL.toExternalForm.replaceAllLiterally("\\", "/") + ";AUTO_SERVER=TRUE")

    if (isNew) {
      val conn = basicDataSource.getConnection
      try {
        CreateSpatialExtension.initSpatialExtension(conn)
      } finally {
        conn.close()
      }
    }

    (basicDataSource, () => basicDataSource.close())



  }

}
