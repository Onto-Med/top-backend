package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.Organisation;
import org.springframework.stereotype.Service;

@Service
public class OrganisationService {
//  @Autowired DSLContext context;

  public Organisation createOrganisation(Organisation organisation) {
    // TODO: use below code to get current user
    // UserDetails userDetails =
    //   (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    //
    // throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    // userDetails.getUsername());

//    DirectoryRecord newRecord =
//        context
//            .newRecord(DIRECTORY)
//            .setName(organisation.getName())
//            .setDescription(organisation.getDescription())
//            .setType("organisation");
//    newRecord.store();
//    newRecord.refresh();
//
//    Organisation created = new Organisation();
//    created.setOrganisationId(newRecord.getDirectoryId());
//    created.setName(newRecord.getName());
//    created.setDescription(newRecord.getDescription());
//    created.setCreatedAt(newRecord.getCreatedAt());

//    return created;
    return null;
  }

  public void deleteOrganisationByName(String organisationName) {
//    DirectoryRecord record = context.fetchOne(DIRECTORY, DIRECTORY.NAME.eq(organisationName));
//    if (record == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//
//    Organisation organisation = new Organisation();
//    organisation.setOrganisationId(record.getDirectoryId());
//    organisation.setName(record.getName());
//    organisation.setDescription(record.getDescription());
//    organisation.setCreatedAt(record.getCreatedAt());
//
//    record.delete();
  }
}
