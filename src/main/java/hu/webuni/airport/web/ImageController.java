package hu.webuni.airport.web;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import hu.webuni.airport.api.ImageControllerApi;
import hu.webuni.airport.repository.ImageRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class ImageController implements ImageControllerApi {

	private final ImageRepository imageRepository;

//	FÁJL LETÖLTÉS - openapi leíróban az alábbiak lettek beállítva:
//
//	  '/api/image/{id}':
//		    parameters:
//		      - schema:
//		          type: integer
//		          format: int64
//		        name: id
//		        in: path
//		        required: true
//		    get:
//		      tags:
//		        - image-controller
//		      summary: ''
//		      operationId: downloadImage
//		      responses:
//		        '200':
//		          description: OK
//		          content:
//		            image/jpeg:
//		              schema:
//		                type: string
//		                format: binary
	
	@Override
	public ResponseEntity<Resource> downloadImage(Long id) {
		
		//nagyobb fájlok esetén nem hatékony adatbázisban tárolni azokat, olyankor célszerű stream-elve kezelni a fel- és letöltést, illetve fájlba menteni a feltöltött fájlt és onnan visszaadni
		//ehhez hasonló módon lehet itt használni FileResource-t / InputStreamResource-t
		//illetve feltöltésnél sem muszáj a content-től a getBytes-cal az összes byte-ot elkérni egyszerre, hanem lehet az inputstream-en keresztül darabonként olvasni és lementeni fájlba

		return ResponseEntity.ok(new ByteArrayResource(imageRepository.findById(id).get().getData()));
		
	}
	
}