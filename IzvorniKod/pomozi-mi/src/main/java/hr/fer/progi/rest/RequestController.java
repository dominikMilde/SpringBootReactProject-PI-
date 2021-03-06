package hr.fer.progi.rest;

import hr.fer.progi.domain.Request;
import hr.fer.progi.domain.RequestStatus;
import hr.fer.progi.domain.User;
import hr.fer.progi.mappers.CreateRequestDTO;
import hr.fer.progi.mappers.FilterDTO;
import hr.fer.progi.mappers.RequestDTO;
import hr.fer.progi.mappers.UserDTO;
import hr.fer.progi.service.*;

import java.util.Comparator;
import java.util.Date;

import hr.fer.progi.service.exceptions.BlockingException;
import hr.fer.progi.service.exceptions.InvalidRequestException;
import hr.fer.progi.service.exceptions.NonexistingObjectReferencedException;
import hr.fer.progi.wrappers.RequestModelAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

/**
 * Handles requests toward "/requests" and "/requests/{id}" path.
 */
@RestController
@RequestMapping("/requests")
public class RequestController {

    @Autowired
    private RequestService requestService;

    @Autowired
    private UserService userService;

    @Autowired
    private final RequestModelAssembler assembler;

    public RequestController(RequestModelAssembler assembler) {
        this.assembler = assembler;
    }


    /**
     * Returns all requests in system encapsulated in a model.
     * Only requests that are ACTNOANS and ACTANS are returned if they haven't expired
     * @return all requests encapsulated in a model
     */
    @PostMapping("/all")
    @Secured("ROLE_USER")
    public CollectionModel<EntityModel<RequestDTO>> getRequests(@RequestBody FilterDTO filterDTO) {
        System.out.println(filterDTO);

        List<RequestDTO> requests = requestService.getFilteredRequests(filterDTO)
                .stream()
                .map(r -> r.mapToRequestDTO())
                .collect(Collectors.toList());

        return assembler.toCollectionModel(requests);
    }

    /**
     * Gets one request whose id equals given id.
     *
     * @param id Id of request we want to find
     * @return request whose id was given
     */
    @GetMapping("/{id}") //TODO handle exception if no request is found
    public EntityModel<RequestDTO> getRequest(@PathVariable("id") Long id) {
        Request r;
        // TODO Refactor this code, so it's more pretty
        try {
            /* This is inside try/catch block because if we send getRequestById()
               an id which is not in the database, it will throw NoSuchElementException */
            r = requestService.getRequestById(id);
        } catch (NoSuchElementException e) {
            throw new InvalidRequestException("Request is either nonexisting.");
        }
        return assembler.toModel(requestService.getRequestById(id)
                .mapToRequestDTO());
    }

    /**
     * Handles HTTP POST Request when user wants to create new request.
     *
     * @param createRequest DTO that contains all necessary data for creating new request.
     * @return
     */
    @PostMapping("")
    @Secured("ROLE_USER")
    public ResponseEntity<RequestDTO> createRequest(@RequestBody CreateRequestDTO createRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        return ResponseEntity.ok(requestService.addRequest(createRequest
                .mapToRequest(userService.findByUsername(username))).mapToRequestDTO());
    }


    /**
     * Admin deletion of request deemed inappropriate
     * @param id of the request to be deleted
     * @return response
     */
    @DeleteMapping("/delete/{id}")
    @Secured("ROLE_ADMIN")
    @Transactional
    public ResponseEntity<?> deleteRequest(@PathVariable("id") Long id) {

        return ResponseEntity.ok(requestService.deleteRequest(id));
    }


    @PostMapping("/settings/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<?> updateRequest(@RequestBody RequestDTO createRequest, @PathVariable("id") Long id) {
        Request r = requestService.getRequestById(id);
        r.updateRequest(createRequest.mapToRequest());

        EntityModel<RequestDTO> model = assembler.toModel(requestService.updateRequest(r).mapToRequestDTO());

        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }


    /**
     * User deletion or blocking of the request
     * User can only block or delete the request where he is an author
     * If request has been responded it can only be blocked, otherwise deleted
     * @param id id of the referenced request
     * @return response
     */
    @PostMapping("/blockDeleteRequest/{id}")
    @Secured("ROLE_USER")
    public ResponseEntity<?> blockOrDeleteRequest(@PathVariable("id") Long id) {

        Request r = requestService.getRequestById(id);

        if (r == null) {
            throw new NonexistingObjectReferencedException("Request does not exist.");
        }
        //if no one answered the request it can be deleted
        if(r.getStatus() == RequestStatus.ACTNOANS)
            return requestService.deleteRequest(r.getId()) ? ResponseEntity.ok("Successful deletion") :
                    ResponseEntity.badRequest().body("Unsuccessful deletion");

        //otherwise it is to be blocked
        if (r.getStatus() == RequestStatus.BLOCKED) {
            throw new BlockingException("Request with id " + id + " is already blocked");
        }

        return ResponseEntity.ok(assembler.toModel(requestService.blockRequest(r).mapToRequestDTO()));
    }


    /**
     * Puts current user as the potential handler for the request
     * @param id of the request that is responded to
     * @return updated request
     */
    @PostMapping("/respond/{id}")
    @Secured("ROLE_USER")
    public EntityModel<RequestDTO> respondToRequest(@PathVariable("id") Long id) {
        Request r = requestService.getRequestById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User potentialHandler = userService.findByUsername(username);

        return assembler.toModel(requestService.requestRespond(r, potentialHandler).mapToRequestDTO());
    }


    /**
     * Picking handler from the potential handlers set
     * @param id id of the request for which handler is picked
     * @param handlerDTO user that is to be picked as a handler
     * @return updated request
     */
    @PostMapping("/pickHandler/{id}")
    @Secured("ROLE_USER")
    public EntityModel<RequestDTO> pickHandler(@PathVariable("id") Long id, @RequestBody UserDTO handlerDTO) {
        Request r = requestService.getRequestById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User current = userService.findByUsername(username);
        if (r.getRequestAuthor() != current) {
            throw new InvalidRequestException("You cant pick handler for request you did not create!");
        }

        System.out.println(handlerDTO.getUsername());
        User handler = userService.findByUsername(handlerDTO.getUsername());

        //if handler is not in database or not in potentialHandler list throw exception
        if( handler==null || !r.getPotentialHandler().contains(handler))
            throw new NonexistingObjectReferencedException("Chosen user is not one of the potential handlers");

        //if handler is current user throw exception
        if( handler.getId().equals(current.getId()))
            throw  new IllegalArgumentException("You cannot chose yourself as a handler");


        return assembler.toModel(requestService.pickRequestHandler(r, handler).mapToRequestDTO());
    }

    /**
     * Marking of the request as done
     * Request can only be marked done if it is in ACCEPTED state, thus has a handler
     * @param id referenced request id
     * @return updated request
     */
    @PostMapping("/markDone/{id}")
    @Secured("ROLE_USER")
    public EntityModel<RequestDTO> markDone(@PathVariable("id") Long id) {
        Request r = requestService.getRequestById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User current = userService.findByUsername(username);
        if (r.getRequestAuthor() != current) {
            throw new InvalidRequestException("You cant mark done the request you did not create!");
        }

        if( !r.getStatus().equals(RequestStatus.ACCEPTED))
            throw new IllegalArgumentException("A request that isn't marked ACCEPTED cannot be marked DONE");



        return assembler.toModel(requestService.markRequestDone(r).mapToRequestDTO());
    }

    /**
     * Rejecting potential handler
     * @param id referenced request id
     * @param handlerDTO the user to be rejected
     * @return updated request
     */
    @PostMapping("/rejectHandler/{id}")
    @Secured("ROLE_USER")
    public EntityModel<RequestDTO> rejectHandler(@PathVariable("id") Long id, @RequestBody UserDTO handlerDTO) {
        Request r = requestService.getRequestById(id);
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User current = userService.findByUsername(username);
        if (r.getRequestAuthor() != current) {
            throw new InvalidRequestException("You cant reject handler for request you did not create!");
        }
        System.out.println(handlerDTO.getUsername());
        User handler = userService.findByUsername(handlerDTO.getUsername());

        if( handler==null)
            throw new NonexistingObjectReferencedException("Referenced user does not exist");

        return assembler.toModel(requestService.denyRequestHandler(r, handler).mapToRequestDTO());
    }


    /**
     * Method for fetching potential handlers of a request
     * @param id id of the referenced request
     * @return list of potentials handlers of the referenced request
     */
    @GetMapping("getPotentialHandlers/{id}")
    @Secured("ROLE_USER")
    public List<UserDTO> getPotentialHandlers(@PathVariable("id") Long id){
        Request r = requestService.getRequestById(id);

        if( r==null)
            throw new NonexistingObjectReferencedException("Nonexisting request referenced");

        return r.mapToRequestDTO().getPotentialHandler().stream().collect(Collectors.toList());

    }

}
