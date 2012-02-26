/**
 * Copyright (c) 2012 Tomáš Polešovský
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package cz.topolik.fsrepo.mapper;

import cz.topolik.fsrepo.LocalFileSystemRepository;

/**
 *
 * @author Tomas Polesovsky
 */
public class FileSystemRepositoryEnvironment {
    private FileSystemRepositoryMapper mapper;
    private FileSystemRepositoryIndexer indexer;
    private LocalFileSystemRepository repository;

    public FileSystemRepositoryIndexer getIndexer() {
        return indexer;
    }

    public void setIndexer(FileSystemRepositoryIndexer indexer) {
        this.indexer = indexer;
    }

    public FileSystemRepositoryMapper getMapper() {
        return mapper;
    }

    public void setMapper(FileSystemRepositoryMapper mapper) {
        this.mapper = mapper;
    }

    public LocalFileSystemRepository getRepository() {
        return repository;
    }

    public void setRepository(LocalFileSystemRepository repository) {
        this.repository = repository;
    }

}
