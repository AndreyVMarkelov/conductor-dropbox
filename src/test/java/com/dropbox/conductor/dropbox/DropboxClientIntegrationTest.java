package com.dropbox.conductor.dropbox;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

class DropboxClientIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "DROPBOX_ACCESS_TOKEN", matches = ".+")
    void authenticatesWithDropbox() throws Exception {
        var client = DropboxClientProvider.create();

        var account = client.users().getCurrentAccount();

        assertNotNull(account.getAccountId());

        System.out.println("Connected to Dropbox account: " + account.getAccountId());
    }
}
