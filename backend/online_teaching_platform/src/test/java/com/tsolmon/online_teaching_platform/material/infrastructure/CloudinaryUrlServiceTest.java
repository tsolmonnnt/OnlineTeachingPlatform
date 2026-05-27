package com.tsolmon.online_teaching_platform.material.infrastructure;



import com.cloudinary.Cloudinary;

import com.tsolmon.online_teaching_platform.material.domain.TeachingMaterial;

import org.junit.jupiter.api.Test;



import java.util.Map;



import static org.assertj.core.api.Assertions.assertThat;



class CloudinaryUrlServiceTest {



    @Test

    void isPdfMaterialShouldDetectByContentTypeAndTitle() {

        TeachingMaterial byType = new TeachingMaterial();

        byType.setContentType("application/pdf");

        assertThat(CloudinaryUrlService.isPdfMaterial(byType)).isTrue();



        TeachingMaterial byTitle = new TeachingMaterial();

        byTitle.setTitle("notes.pdf");

        assertThat(CloudinaryUrlService.isPdfMaterial(byTitle)).isTrue();

    }



    @Test

    void publicRawUploadPdfShouldReturnStoredSecureUrlWithoutResigning() {

        CloudinaryProperties props = new CloudinaryProperties(

                "dwfb98ftt",

                "test-key",

                "test-secret",

                "subjectFiles",

                null,

                null,

                null

        );

        CloudinaryUrlService service = new CloudinaryUrlService(new Cloudinary(Map.of(

                "cloud_name", "dwfb98ftt",

                "api_key", "test-key",

                "api_secret", "test-secret"

        )), props);



        String storedUrl =

                "https://res.cloudinary.com/dwfb98ftt/raw/upload/v1779868739/subjectFiles/kmduhoaxp92ruk6v2upt.pdf";

        TeachingMaterial material = new TeachingMaterial();

        material.setCloudinaryPublicId("subjectFiles/kmduhoaxp92ruk6v2upt.pdf");

        material.setSecureUrl(storedUrl);

        material.setContentType("application/pdf");

        material.setCloudinaryResourceType("raw");

        material.setCloudinaryDeliveryType("upload");

        material.setCloudinaryVersion(1_779_868_739L);



        String url = service.deliveryUrl(material);



        assertThat(url).isEqualTo(storedUrl);

        assertThat(url).doesNotContain("/s--");

        assertThat(url).doesNotContain(".pdf.pdf");

    }



    @Test

    void authenticatedMaterialShouldProduceSignedUrl() {

        CloudinaryProperties props = new CloudinaryProperties(

                "dwfb98ftt",

                "test-key",

                "test-secret",

                "subjectFiles",

                null,

                null,

                null

        );

        CloudinaryUrlService service = new CloudinaryUrlService(new Cloudinary(Map.of(

                "cloud_name", "dwfb98ftt",

                "api_key", "test-key",

                "api_secret", "test-secret"

        )), props);



        TeachingMaterial material = new TeachingMaterial();

        material.setCloudinaryPublicId("subjectFiles/private-doc.pdf");

        material.setSecureUrl(

                "https://res.cloudinary.com/dwfb98ftt/raw/authenticated/v1779868739/subjectFiles/private-doc.pdf"

        );

        material.setContentType("application/pdf");

        material.setCloudinaryResourceType("raw");

        material.setCloudinaryDeliveryType("authenticated");

        material.setCloudinaryVersion(1_779_868_739L);



        assertThat(CloudinaryUrlService.requiresSignedDelivery(material)).isTrue();



        String url = service.deliveryUrl(material);



        assertThat(url).isNotNull();

        assertThat(url).contains("/raw/authenticated/");

        assertThat(url).contains("/s--");

        assertThat(url).endsWith(".pdf");

        assertThat(url).doesNotContain(".pdf.pdf");

    }



    @Test

    void legacyImageUploadPdfShouldProduceSignedUrlWhenAuthenticated() {

        CloudinaryProperties props = new CloudinaryProperties(

                "dwfb98ftt",

                "test-key",

                "test-secret",

                "subjectFiles",

                null,

                null,

                null

        );

        CloudinaryUrlService service = new CloudinaryUrlService(new Cloudinary(Map.of(

                "cloud_name", "dwfb98ftt",

                "api_key", "test-key",

                "api_secret", "test-secret"

        )), props);



        TeachingMaterial material = new TeachingMaterial();

        material.setCloudinaryPublicId("subjectFiles/tfo3sox7riwhldekrqyl");

        material.setSecureUrl(

                "https://res.cloudinary.com/dwfb98ftt/image/authenticated/v1779865892/subjectFiles/tfo3sox7riwhldekrqyl.pdf"

        );

        material.setContentType("application/pdf");

        material.setCloudinaryResourceType("image");

        material.setCloudinaryDeliveryType("authenticated");

        material.setCloudinaryVersion(1_779_865_892L);



        String url = service.deliveryUrl(material);



        assertThat(url).isNotNull();

        assertThat(url).contains("/image/authenticated/");

        assertThat(url).endsWith(".pdf");

        assertThat(url).doesNotContain(".pdf.pdf");

        assertThat(url).contains("/s--");

    }



    @Test

    void publicRawUploadPdfWithoutExtensionShouldAppendPdfForInlinePreview() {

        CloudinaryProperties props = new CloudinaryProperties(

                "dwfb98ftt",

                "test-key",

                "test-secret",

                "subjectFiles",

                null,

                null,

                null

        );

        CloudinaryUrlService service = new CloudinaryUrlService(new Cloudinary(Map.of(

                "cloud_name", "dwfb98ftt",

                "api_key", "test-key",

                "api_secret", "test-secret"

        )), props);



        String storedUrl =

                "https://res.cloudinary.com/dwfb98ftt/raw/upload/v1779868739/subjectFiles/kmduhoaxp92ruk6v2upt";

        TeachingMaterial material = new TeachingMaterial();

        material.setCloudinaryPublicId("subjectFiles/kmduhoaxp92ruk6v2upt");

        material.setSecureUrl(storedUrl);

        material.setContentType("application/pdf");

        material.setCloudinaryResourceType("raw");

        material.setCloudinaryDeliveryType("upload");

        material.setCloudinaryVersion(1_779_868_739L);



        assertThat(CloudinaryUrlService.needsPdfExtensionInUrl(material)).isTrue();



        String url = service.deliveryUrl(material);



        assertThat(url).isNotEqualTo(storedUrl);

        assertThat(url).endsWith(".pdf");

        assertThat(url).contains("/raw/upload/");

        assertThat(url).doesNotContain("/s--");

        assertThat(url).doesNotContain(".pdf.pdf");

    }



    @Test

    void authenticatedPdfWithoutExtensionShouldProduceSignedUrlEndingWithPdf() {

        CloudinaryProperties props = new CloudinaryProperties(

                "dwfb98ftt",

                "test-key",

                "test-secret",

                "subjectFiles",

                null,

                null,

                null

        );

        CloudinaryUrlService service = new CloudinaryUrlService(new Cloudinary(Map.of(

                "cloud_name", "dwfb98ftt",

                "api_key", "test-key",

                "api_secret", "test-secret"

        )), props);



        TeachingMaterial material = new TeachingMaterial();

        material.setCloudinaryPublicId("subjectFiles/private-doc");

        material.setSecureUrl(

                "https://res.cloudinary.com/dwfb98ftt/raw/authenticated/v1779868739/subjectFiles/private-doc"

        );

        material.setContentType("application/pdf");

        material.setCloudinaryResourceType("raw");

        material.setCloudinaryDeliveryType("authenticated");

        material.setCloudinaryVersion(1_779_868_739L);



        String url = service.deliveryUrl(material);



        assertThat(url).isNotNull();

        assertThat(url).contains("/raw/authenticated/");

        assertThat(url).contains("/s--");

        assertThat(url).endsWith(".pdf");

        assertThat(url).doesNotContain(".pdf.pdf");

    }



    @Test

    void publicIdHasExtensionShouldDetectPdfSuffix() {

        assertThat(CloudinaryUrlService.publicIdHasExtension("subjectFiles/file.pdf", "pdf")).isTrue();

        assertThat(CloudinaryUrlService.publicIdHasExtension("subjectFiles/file", "pdf")).isFalse();

    }



    @Test

    void parseVersionFromUrlShouldReadCloudinaryVersionSegment() {

        assertThat(CloudinaryUrlService.parseVersionFromUrl(

                "https://res.cloudinary.com/demo/image/upload/v1779865892/folder/file.pdf"

        )).isEqualTo(1_779_865_892L);

    }

}


