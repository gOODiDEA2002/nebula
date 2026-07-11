package io.nebula.storage.minio;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObjectListingTest {

    @Test
    void directoryEntryWithoutLastModifiedCanBeListed() throws Exception {
        MinioClient client = mock(MinioClient.class);
        @SuppressWarnings("unchecked")
        Result<Item> result = mock(Result.class);
        Item item = mock(Item.class);
        when(client.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of(result));
        when(result.get()).thenReturn(item);
        when(item.objectName()).thenReturn("folder/");
        when(item.isDir()).thenReturn(true);

        var objects = new MinIOStorageService(client).listObjects("bucket", "folder/");

        assertThat(objects).singleElement().satisfies(summary -> {
            assertThat(summary.isDirectory()).isTrue();
            assertThat(summary.getLastModified()).isNull();
        });
        verify(item, never()).lastModified();
        ArgumentCaptor<ListObjectsArgs> args = ArgumentCaptor.forClass(ListObjectsArgs.class);
        verify(client).listObjects(args.capture());
        assertThat(args.getValue().recursive()).isTrue();
    }
}
