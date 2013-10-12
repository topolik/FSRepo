package cz.topolik.liferay.fsrepo;

import com.liferay.portal.kernel.repository.DefaultLocalRepositoryImpl;

/**
 * @author Tomas Polesovsky
 */
public class FSRepoLocalRepositoryImpl extends DefaultLocalRepositoryImpl {

    public FSRepoLocalRepositoryImpl() {
        super(new FSRepo());
    }

}
